package com.morpho.android.usb;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;

public class USBDeviceAttributes implements Parcelable {
    public static Creator<USBDeviceAttributes> CREATOR = new C02861();
    private final int interfaceNumber;
    private String mFreindlyName;
    private String manufacturer;
    private final String path;
    private String product;
    private final int productId;
    private final int vendorId;

    static class C02861 implements Creator<USBDeviceAttributes> {
        C02861() {
        }

        public USBDeviceAttributes createFromParcel(Parcel parcel) {
            return new USBDeviceAttributes(parcel);
        }

        public USBDeviceAttributes[] newArray(int size) {
            return new USBDeviceAttributes[size];
        }
    }

    protected USBDeviceAttributes(int vendorId, int productId) {
        this.manufacturer = null;
        this.product = null;
        this.mFreindlyName = "";
        this.vendorId = vendorId;
        this.productId = productId;
        this.path = null;
        this.interfaceNumber = 0;
    }

    private USBDeviceAttributes(Parcel parcel) {
        this.manufacturer = null;
        this.product = null;
        this.mFreindlyName = "";
        if (parcel != null) {
            this.path = parcel.readString();
            this.vendorId = parcel.readInt();
            this.productId = parcel.readInt();
            this.interfaceNumber = parcel.readInt();
            return;
        }
        this.vendorId = 0;
        this.productId = 0;
        this.path = "";
        this.interfaceNumber = 0;
    }

    public int getVendorId() {
        return this.vendorId;
    }

    public String getProduct() {
        return this.product;
    }

    public int getInterfaceNumber() {
        return this.interfaceNumber;
    }

    public int getProductId() {
        return this.productId;
    }

    public String getManufacturer() {
        return this.manufacturer;
    }

    public String getPath() {
        return this.path;
    }

    public void setProduct(String product) {
        this.product = product;
    }

    public USBDeviceAttributes(String path, int vendorId, int productId, int interfaceNumber) {
        this.manufacturer = null;
        this.product = null;
        this.mFreindlyName = "";
        this.path = path;
        this.vendorId = vendorId;
        this.productId = productId;
        this.interfaceNumber = interfaceNumber;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        if (dest != null) {
            dest.writeString(getPath());
            dest.writeInt(getInterfaceNumber());
            dest.writeInt(getProductId());
            dest.writeInt(getVendorId());
        }
    }

    public String getFreindlyName() {
        return this.mFreindlyName;
    }

    public void setFreindlyName(String freindlyName) {
        this.mFreindlyName = freindlyName;
    }
}
