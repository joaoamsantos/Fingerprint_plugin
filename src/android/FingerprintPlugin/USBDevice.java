package FingerprintPlugin;

import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.util.Log;
import java.io.UnsupportedEncodingException;

import FingerprintPlugin.*;

public class USBDevice {
    public static UsbManager mManager = null;
    public USBDeviceAttributes mAttributes = null;
    public UsbDeviceConnection mConnection = null;
    public UsbDevice mDevice = null;
    private byte[] mDeviceDescriptor = null;
    public UsbEndpoint mEndpointIn = null;
    public UsbEndpoint mEndpointOut = null;
    public String mFreindlyName = "";
    public UsbInterface mInterface = null;
    public int mMaxPacketInSize = 0;
    public int mMaxPacketOutSize = 0;

    public synchronized boolean hasPermission() {
        boolean z;
        if (this.mDevice == null || mManager == null) {
            z = false;
        } else {
            z = mManager.hasPermission(this.mDevice);
        }
        return z;
    }

    public synchronized int open() throws Exception {
        int i;
        if (this.mDevice == null || mManager == null) {
            throw new Exception("Failuire to open device: either usb manager or connection null");
        }
        this.mInterface = this.mDevice.getInterface(getAttributes().getInterfaceNumber());
        this.mConnection = mManager.openDevice(this.mDevice);
        if (this.mConnection != null) {
            this.mConnection.claimInterface(this.mInterface, true);
            i = 0;
        } else {
            i = -6;
        }
        return i;
    }

    public synchronized UsbInterface CreateInterface(int interfaceNumber) {
        return this.mDevice.getInterface(interfaceNumber);
    }

    public USBDevice(USBDeviceAttributes iAttributes, UsbManager iManager, UsbDevice iDevice) {
        this.mDevice = iDevice;
        this.mAttributes = iAttributes;
        mManager = iManager;
    }

    public synchronized String getProductString() throws Exception {
        if (getAttributes().getProduct() == null) {
            byte[] pdt = new byte[64];
            getStringDescriptor(pdt, getDeviceDescriptor()[15]);
            getAttributes().setProduct(convertDescriptorUnicodeLEToString(pdt));
        }
        return getAttributes().getProduct();
    }

    private synchronized String convertDescriptorUnicodeLEToString(byte[] buffer) {
        String str;
        try {
            int bufferLength = buffer[0];
            byte[] subStr = new byte[(bufferLength - 2)];
            System.arraycopy(buffer, 2, subStr, 0, bufferLength - 2);
            str = new String(subStr, "UTF-16LE");
        } catch (UnsupportedEncodingException e) {
            str = "not available";
        } catch (Exception ex) {
            Log.e("USBDevice.convertDescriptorUnicodeLEToString", ex.getMessage());
            str = "not available";
        }
        return str;
    }

    public synchronized int findEndPoint() {
        int i = 0;
        synchronized (this) {
            if (this.mInterface == null) {
                i = -3;
            } else if (this.mInterface.getEndpointCount() == 2) {
                UsbEndpoint lEndPoint0 = this.mInterface.getEndpoint(0);
                UsbEndpoint lEndPoint1 = this.mInterface.getEndpoint(1);
                if ((lEndPoint0.getAttributes() & 3) == 2 && (lEndPoint1.getAttributes() & 3) == 2) {
                    if ((lEndPoint0.getAddress() & 128) != 0 && (lEndPoint1.getAddress() & 128) == 0) {
                        this.mEndpointOut = lEndPoint1;
                        this.mEndpointIn = lEndPoint0;
                        this.mMaxPacketInSize = lEndPoint0.getMaxPacketSize();
                        this.mMaxPacketOutSize = lEndPoint1.getMaxPacketSize();
                    } else if ((lEndPoint0.getAddress() & 128) == 0 && (lEndPoint1.getAddress() & 128) != 0) {
                        this.mEndpointOut = lEndPoint0;
                        this.mEndpointIn = lEndPoint1;
                        this.mMaxPacketInSize = lEndPoint1.getMaxPacketSize();
                        this.mMaxPacketOutSize = lEndPoint0.getMaxPacketSize();
                    }
                }
                i = -1;
            } else {
                i = -2;
            }
        }
        return i;
    }

    public synchronized USBDeviceAttributes getAttributes() {
        return this.mAttributes;
    }

    private synchronized int getStringDescriptor(byte[] buffer, int index) throws Exception {
        return getDescriptor(buffer, 768, index);
    }

    private synchronized byte[] getDeviceDescriptor() throws Exception {
        if (this.mDeviceDescriptor == null) {
            this.mDeviceDescriptor = new byte[18];
            getDescriptor(this.mDeviceDescriptor, 256, 1);
        }
        return this.mDeviceDescriptor;
    }

    private synchronized int getDescriptor(byte[] buffer, int descType, int index) throws Exception {
        int res = 0;
        if (buffer != null) {
            if (!(this.mConnection == null || this.mInterface == null)) {
                if (this.mConnection.claimInterface(this.mInterface, true)) {
                    byte[] desc = new byte[buffer.length];
                    res = this.mConnection.controlTransfer(128, 6, descType | index, USBConstants.USB_DEVICE_DESCRIPTOR_LANGUAGE_ENGLISH, desc, buffer.length, 1000);
                    if (res < 0) {
                        throw new Exception("getDescriptor operation is unsuccessful. Descriptor index=" + index + "Descriptor type=" + descType);
                    }
                    System.arraycopy(desc, 0, buffer, 0, desc.length);
                } else {
                    throw new Exception("Could not claim the USB device interface");
                }
            }
        }
        return res;
    }

    public synchronized int close() {
        int i;
        try {
            if (this.mConnection != null) {
                this.mConnection.releaseInterface(this.mInterface);
                this.mConnection.close();
                this.mInterface = null;
                this.mConnection = null;
            }
            i = 0;
        } catch (Exception e) {
            Log.e("USBDevice close", e.getMessage());
            i = -5;
        }
        return i;
    }

    public synchronized UsbInterface claimInterface() {
        return this.mInterface;
    }

    public synchronized int write(byte[] data, int datasize, int timeout) {
        int ret;
        if (this.mConnection != null) {
            this.mConnection.claimInterface(this.mInterface, true);
            if (datasize <= 0 || datasize % this.mMaxPacketOutSize != 0 || this.mConnection.bulkTransfer(this.mEndpointOut, data, 0, 1) >= 0) {
                ret = this.mConnection.bulkTransfer(this.mEndpointOut, data, datasize, timeout);
                this.mConnection.releaseInterface(this.mInterface);
            } else {
                this.mConnection.releaseInterface(this.mInterface);
                ret = -7;
            }
        } else {
            ret = -6;
        }
        return ret;
    }

    public synchronized int read(byte[] data, int datasize, int timeout) {
        int ret;
        if (this.mConnection != null) {
            this.mConnection.claimInterface(this.mInterface, true);
            if (this.mEndpointIn == null) {
                Log.e("MORPHO_USB", "null read endpoint !");
            }
            ret = this.mConnection.bulkTransfer(this.mEndpointIn, data, datasize, timeout);
            this.mConnection.releaseInterface(this.mInterface);
        } else {
            ret = -6;
        }
        return ret;
    }

    public synchronized int getStringSimple(byte[] buffer, int index) {
        int stringDescriptor;
        try {
            stringDescriptor = getStringDescriptor(buffer, index);
        } catch (Exception e) {
            Log.e("USBDevice getStringSimple", e.getMessage());
            stringDescriptor = -4;
        }
        return stringDescriptor;
    }

    public synchronized String getSerialNumberString() {
        String serial;
        if (this.mConnection != null) {
            serial = this.mConnection.getSerial();
        } else {
            serial = "Serial not available";
        }
        return serial;
    }

    public synchronized int getMaxPacketInSize() {
        return this.mMaxPacketInSize;
    }

    public int getMaxPacketOutSize() {
        return this.mMaxPacketOutSize;
    }
}
