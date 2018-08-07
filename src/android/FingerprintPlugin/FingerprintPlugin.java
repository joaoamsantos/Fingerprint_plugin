package FingerprintPlugin;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.hoho.android.usbserial.driver.CdcAcmSerialDriver;
import com.hoho.android.usbserial.driver.Ch34xSerialDriver;
import com.hoho.android.usbserial.driver.Cp21xxSerialDriver;
import com.hoho.android.usbserial.driver.FtdiSerialDriver;
import com.hoho.android.usbserial.driver.ProbeTable;
import com.hoho.android.usbserial.driver.ProlificSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.util.Base64;
import android.util.Log;

import FingerprintPlugin.*;

/**
 * Cordova plugin to communicate with the android serial port
 * @author Joao Miguel Santos <joaomsantos@deloitte.pt>
 */
public class FingerprintPlugin extends CordovaPlugin {
	
	static volatile List<USBDevice> deviceList = null;
	
	// logging tag
	private final String TAG = FingerprintPlugin.class.getSimpleName();
	// actions definitions
	private static final String ACTION_REQUEST_PERMISSION = "requestPermission";
	private static final String ACTION_OPEN = "openSerial";
	private static final String ACTION_READ = "readSerial";
	private static final String ACTION_CLOSE = "closeSerial";
	private static final String ACTION_READ_CALLBACK = "registerReadCallback";
	private static final String ACTION_DEVICES_HAS_PERMISSION = "isDevicesHasPermission";

	public static final String SOFTWAREID_CBM = "CBM";
	public static final String SOFTWAREID_CBME3 = "CBM-E3";
    	public static final String SOFTWAREID_CBME3L = "CBM-E3L";
    	public static final String SOFTWAREID_FVP = "MSO FVP";
    	public static final String SOFTWAREID_FVP_C = "MSO FVP_C";
    	public static final String SOFTWAREID_FVP_CL = "MSO FVP_CL";
    	public static final String SOFTWAREID_MASIGMA = "MA SIGMA";
    	public static final String SOFTWAREID_MEP = "MEPUSB";
    	public static final String SOFTWAREID_MSO100 = "MSO100";
    	public static final String SOFTWAREID_MSO1300E3 = "MSO1300-E3";
    	public static final String SOFTWAREID_MSO1300E3L = "MSO1300-E3L";
    	public static final String SOFTWAREID_MSO1350 = "MSO1350";
    	public static final String SOFTWAREID_MSO1350E3 = "MSO1350-E3";
    	public static final String SOFTWAREID_MSO1350E3L = "MSO1350-E3L";
    	public static final String SOFTWAREID_MSO300 = "MSO300";
    	public static final String SOFTWAREID_MSO350 = "MSO350";
    	public static final String SOFTWAREID_MSOTEST = "MSOTEST";


	// UsbManager instance to deal with permission and opening
	private UsbManager manager;
	// The current driver that handle the serial port
	private UsbSerialDriver driver;
	// The serial port that will be used in this plugin
	private UsbSerialPort port;
	// Read buffer, and read params
	private static final int READ_WAIT_MILLIS = 200;
	private static final int BUFSIZ = 4096;
	private final ByteBuffer mReadBuffer = ByteBuffer.allocate(BUFSIZ);
	// Connection info
	private int baudRate;
	private int dataBits;
	private int stopBits;
	private int parity;
	private boolean setDTR;
	private boolean setRTS;
	private boolean sleepOnPause;

	static {
        System.loadLibrary("/FingerprintPlugin/libs/NativeMorphoSmartSDK_6.13.3.0-5.1.so");
        System.loadLibrary("/FingerprintPlugin/libs/MSO100.so");
    }
	
	// callback that will be used to send back data to the cordova app
	private CallbackContext readCallback;
	
	// I/O manager to handle new incoming serial data
	private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();
	private SerialInputOutputManager mSerialIoManager;
	private final SerialInputOutputManager.Listener mListener =
			new SerialInputOutputManager.Listener() {
				@Override
				public void onRunError(Exception e) {
					Log.d(TAG, "Runner stopped.");
				}
				@Override
				public void onNewData(final byte[] data) {
					FingerprintPlugin.this.updateReceivedData(data);
				}
			};

	/**
	 * Overridden execute method
	 * @param action the string representation of the action to execute
	 * @param args
	 * @param callbackContext the cordova {@link CallbackContext}
	 * @return true if the action exists, false otherwise
	 * @throws JSONException if the args parsing fails
	 */
	@Override
	public boolean execute(String action, JSONArray args, final CallbackContext callbackContext) throws JSONException {
		Log.d(TAG, "Action: " + action);
		JSONObject arg_object = args.optJSONObject(0);
		// request permission
		if (ACTION_REQUEST_PERMISSION.equals(action)) {
			JSONObject opts = arg_object.has("opts")? arg_object.getJSONObject("opts") : new JSONObject();
			requestPermission(opts, callbackContext);
			return true;
		}
		// open serial port
		else if (ACTION_OPEN.equals(action)) {
			JSONObject opts = arg_object.has("opts")? arg_object.getJSONObject("opts") : new JSONObject();
			openSerial(opts, callbackContext);
			return true;
		}
		// read on the serial port
		else if (ACTION_READ.equals(action)) {
			readSerial(callbackContext);
			return true;
		}
		// close the serial port
		else if (ACTION_CLOSE.equals(action)) {
			closeSerial(callbackContext);
			return true;
		}
		// Register read callback
		else if (ACTION_READ_CALLBACK.equals(action)) {
			registerReadCallback(callbackContext);
			return true;
		}

		else if (ACTION_DEVICES_HAS_PERMISSION.equals(action)) {
			isDevicesHasPermission(opts,callbackContext);
			return true;
		}
		// the action doesn't exist
		return false;
	}

	/**
	 * Request permission the the user for the app to use the USB/serial port
	 * @param callbackContext the cordova {@link CallbackContext}
	 */
	private void requestPermission(final JSONObject opts, final CallbackContext callbackContext) {
		cordova.getThreadPool().execute(new Runnable() {
			public void run() {
				// get UsbManager from Android
				manager = (UsbManager) cordova.getActivity().getSystemService(Context.USB_SERVICE);
				UsbSerialProber prober;

				if (opts.has("vid") && opts.has("pid")) {
					ProbeTable customTable = new ProbeTable();
					Object o_vid = opts.opt("vid"); //can be an integer Number or a hex String
					Object o_pid = opts.opt("pid"); //can be an integer Number or a hex String
					int vid = o_vid instanceof Number ? ((Number) o_vid).intValue() : Integer.parseInt((String) o_vid,16);
					int pid = o_pid instanceof Number ? ((Number) o_pid).intValue() : Integer.parseInt((String) o_pid,16);
					String driver = opts.has("driver") ? (String) opts.opt("driver") : "CdcAcmSerialDriver";

					if (driver.equals("FtdiSerialDriver")) {
						customTable.addProduct(vid, pid, FtdiSerialDriver.class);
					}
					else if (driver.equals("CdcAcmSerialDriver")) {
						customTable.addProduct(vid, pid, CdcAcmSerialDriver.class);
					}
					else if (driver.equals("Cp21xxSerialDriver")) {
                    	customTable.addProduct(vid, pid, Cp21xxSerialDriver.class);
					}
					else if (driver.equals("ProlificSerialDriver")) {
                    	customTable.addProduct(vid, pid, ProlificSerialDriver.class);
					}
					else if (driver.equals("Ch34xSerialDriver")) {
						customTable.addProduct(vid, pid, Ch34xSerialDriver.class);
					}
                    else {
                        Log.d(TAG, "Unknown driver!");
                        callbackContext.error("Unknown driver!");
                    }

					prober = new UsbSerialProber(customTable);

				}
				else {
					// find all available drivers from attached devices.
					prober = UsbSerialProber.getDefaultProber();
				}

				List<UsbSerialDriver> availableDrivers = prober.findAllDrivers(manager);

				if (!availableDrivers.isEmpty()) {
					// get the first one as there is a high chance that there is no more than one usb device attached to your android
					driver = availableDrivers.get(0);
					UsbDevice device = driver.getDevice();
					// create the intent that will be used to get the permission
					PendingIntent pendingIntent = PendingIntent.getBroadcast(cordova.getActivity(), 0, new Intent(UsbBroadcastReceiver.USB_PERMISSION), 0);
					// and a filter on the permission we ask
					IntentFilter filter = new IntentFilter();
					filter.addAction(UsbBroadcastReceiver.USB_PERMISSION);
					// this broadcast receiver will handle the permission results
					UsbBroadcastReceiver usbReceiver = new UsbBroadcastReceiver(callbackContext, cordova.getActivity());
					cordova.getActivity().registerReceiver(usbReceiver, filter);
					// finally ask for the permission
					manager.requestPermission(device, pendingIntent);
				}
				else {
					// no available drivers
					Log.d(TAG, "No device found!");
					callbackContext.error("No device found!");
				}
			}
		});
	}

	/**
	 * Open the serial port from Cordova
	 * @param opts a {@link JSONObject} containing the connection paramters
	 * @param callbackContext the cordova {@link CallbackContext}
	 */
	private void openSerial(final JSONObject opts, final CallbackContext callbackContext) {
		cordova.getThreadPool().execute(new Runnable() {
			public void run() {
				UsbDeviceConnection connection = manager.openDevice(driver.getDevice());
				if (connection != null) {
					// get first port and open it
					port = driver.getPorts().get(0);
					try {
						// get connection params or the default values
						baudRate = opts.has("baudRate") ? opts.getInt("baudRate") : 9600;
						dataBits = opts.has("dataBits") ? opts.getInt("dataBits") : UsbSerialPort.DATABITS_8;
						stopBits = opts.has("stopBits") ? opts.getInt("stopBits") : UsbSerialPort.STOPBITS_1;
						parity = opts.has("parity") ? opts.getInt("parity") : UsbSerialPort.PARITY_NONE;
						setDTR = opts.has("dtr") && opts.getBoolean("dtr");
						setRTS = opts.has("rts") && opts.getBoolean("rts");
						// Sleep On Pause defaults to true
						sleepOnPause = opts.has("sleepOnPause") ? opts.getBoolean("sleepOnPause") : true;

						port.open(connection);
						port.setParameters(baudRate, dataBits, stopBits, parity);
						if (setDTR) port.setDTR(true);
						if (setRTS) port.setRTS(true);
					}
					catch (IOException  e) {
						// deal with error
						Log.d(TAG, e.getMessage());
						callbackContext.error(e.getMessage());
					}
					catch (JSONException e) {
						// deal with error
						Log.d(TAG, e.getMessage());
						callbackContext.error(e.getMessage());
					}

					Log.d(TAG, "Serial port opened!");
					callbackContext.success("Serial port opened!");
				}
				else {
					Log.d(TAG, "Cannot connect to the device!");
					callbackContext.error("Cannot connect to the device!");
				}
				onDeviceStateChange();
			}
		});
	}

	/**
	 * Read on the serial port
	 * @param callbackContext the {@link CallbackContext}
	 */
	private void readSerial(final CallbackContext callbackContext) {
		cordova.getThreadPool().execute(new Runnable() {
			public void run() {
				if (port == null) {
					callbackContext.error("Reading a closed port.");
				} 
				else {
					try {
						int len = port.read(mReadBuffer.array(), READ_WAIT_MILLIS);
						// Whatever happens, we send an "OK" result, up to the
						// receiver to check that len > 0
						PluginResult.Status status = PluginResult.Status.OK;
						if (len > 0) {
							Log.d(TAG, "Read data len=" + len);
							final byte[] data = new byte[len];
							mReadBuffer.get(data, 0, len);
							mReadBuffer.clear();
							callbackContext.sendPluginResult(new PluginResult(status,data));
						}
						else {
							final byte[] data = new byte[0];
							callbackContext.sendPluginResult(new PluginResult(status, data));
						}
					}
					catch (IOException e) {
						// deal with error
						Log.d(TAG, e.getMessage());
						callbackContext.error(e.getMessage());
					}
				}
			}
		});
	}

	/**
	 * Close the serial port
	 * @param callbackContext the cordova {@link CallbackContext}
	 */
	private void closeSerial(final CallbackContext callbackContext) {
		cordova.getThreadPool().execute(new Runnable() {
			public void run() {
				try {
					// Make sure we don't die if we try to close an non-existing port!
					if (port != null) {
						port.close();
					}
					port = null;
					callbackContext.success();
				}
				catch (IOException e) {
					// deal with error
					Log.d(TAG, e.getMessage());
					callbackContext.error(e.getMessage());
				}
				onDeviceStateChange();
			}
		});
	}


	/**
	 * Stop observing serial connection
	 */
	private void stopIoManager() {
		if (mSerialIoManager != null) {
			Log.i(TAG, "Stopping io manager.");
			mSerialIoManager.stop();
			mSerialIoManager = null;
		}
	}

	/**
	 * Observe serial connection
	 */
	private void startIoManager() {
		if (driver != null) {
			Log.i(TAG, "Starting io manager.");
			mSerialIoManager = new SerialInputOutputManager(port, mListener);
			mExecutor.submit(mSerialIoManager);
		}
	}

	/**
	 * Restart the observation of the serial connection
	 */
	private void onDeviceStateChange() {
		stopIoManager();
		startIoManager();
	}

	/**
	 * Dispatch read data to javascript
	 * @param data the array of bytes to dispatch
	 */
	private void updateReceivedData(byte[] data) {
		if( readCallback != null ) {
			PluginResult result = new PluginResult(PluginResult.Status.OK, data);
			result.setKeepCallback(true);
			readCallback.sendPluginResult(result);
		}
	}

	/**
	 * Register callback for read data
	 * @param callbackContext the cordova {@link CallbackContext}
	 */
	private void registerReadCallback(final CallbackContext callbackContext) {
		Log.d(TAG, "Registering callback");
		cordova.getThreadPool().execute(new Runnable() {
			public void run() {
				Log.d(TAG, "Registering Read Callback");
				readCallback = callbackContext;
				JSONObject returnObj = new JSONObject();
				addProperty(returnObj, "registerReadCallback", "true");
				// Keep the callback
				PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, returnObj);
				pluginResult.setKeepCallback(true);
				callbackContext.sendPluginResult(pluginResult);
			}
		});
	}

	/** 
	 * Paused activity handler
	 * @see org.apache.cordova.CordovaPlugin#onPause(boolean)
	 */
	@Override
	public void onPause(boolean multitasking) {
		if (sleepOnPause) {
			stopIoManager();
			if (port != null) {
				try {
					port.close();
				} catch (IOException e) {
					// Ignore
				}
				port = null;
			}
		}
	}

	
	/**
	 * Resumed activity handler
	 * @see org.apache.cordova.CordovaPlugin#onResume(boolean)
	 */
	@Override
	public void onResume(boolean multitasking) {
		Log.d(TAG, "Resumed, driver=" + driver);
		if (sleepOnPause) {
			if (driver == null) {
				Log.d(TAG, "No serial device to resume.");
			} 
			else {
				UsbDeviceConnection connection = manager.openDevice(driver.getDevice());
				if (connection != null) {
					// get first port and open it
					port = driver.getPorts().get(0);
					try {
						port.open(connection);
						port.setParameters(baudRate, dataBits, stopBits, parity);
						if (setDTR) port.setDTR(true);
						if (setRTS) port.setRTS(true);
					}
					catch (IOException  e) {
						// deal with error
						Log.d(TAG, e.getMessage());
					}
					Log.d(TAG, "Serial port opened!");
				}
				else {
					Log.d(TAG, "Cannot connect to the device!");
				}
				Log.d(TAG, "Serial device: " + driver.getClass().getSimpleName());
			}
			
			onDeviceStateChange();
		}
	}


	/**
	 * Utility method to add some properties to a {@link JSONObject}
	 * @param obj the json object where to add the new property
	 * @param key property key
	 * @param value value of the property
	 */
	private void addProperty(JSONObject obj, String key, Object value) {
		try {
			obj.put(key, value);
		}
		catch (JSONException e){}
	}

	private void isDevicesHasPermission(final JSONObject opts, final CallbackContext callbackContext) {
		cordova.getThreadPool().execute(new Runnable() {
			public void run() {
				// get UsbManager from Android
				manager = (UsbManager) cordova.getActivity().getSystemService(Context.USB_SERVICE);
				UsbSerialProber prober;

				if (opts.has("vid") && opts.has("pid")) {
					ProbeTable customTable = new ProbeTable();
					Object o_vid = opts.opt("vid"); //can be an integer Number or a hex String
					Object o_pid = opts.opt("pid"); //can be an integer Number or a hex String
					int vid = o_vid instanceof Number ? ((Number) o_vid).intValue() : Integer.parseInt((String) o_vid,16);
					int pid = o_pid instanceof Number ? ((Number) o_pid).intValue() : Integer.parseInt((String) o_pid,16);
					String driver = opts.has("driver") ? (String) opts.opt("driver") : "CdcAcmSerialDriver";

					if (driver.equals("FtdiSerialDriver")) {
						customTable.addProduct(vid, pid, FtdiSerialDriver.class);
					}
					else if (driver.equals("CdcAcmSerialDriver")) {
						customTable.addProduct(vid, pid, CdcAcmSerialDriver.class);
					}
					else if (driver.equals("Cp21xxSerialDriver")) {
                    	customTable.addProduct(vid, pid, Cp21xxSerialDriver.class);
					}
					else if (driver.equals("ProlificSerialDriver")) {
                    	customTable.addProduct(vid, pid, ProlificSerialDriver.class);
					}
					else if (driver.equals("Ch34xSerialDriver")) {
						customTable.addProduct(vid, pid, Ch34xSerialDriver.class);
					}
                    else {
                        Log.d(TAG, "Unknown driver!");
                        callbackContext.error("Unknown driver!");
                    }

					prober = new UsbSerialProber(customTable);

				}
				else {
					// find all available drivers from attached devices.
					prober = UsbSerialProber.getDefaultProber();
				}

				List<UsbSerialDriver> availableDrivers = prober.findAllDrivers(manager);

				if (!availableDrivers.isEmpty()) {
					// get the first one as there is a high chance that there is no more than one usb device attached to your android
					driver = availableDrivers.get(0);
					UsbDevice device = driver.getDevice();
					// create the intent that will be used to get the permission
					PendingIntent pendingIntent = PendingIntent.getBroadcast(cordova.getActivity(), 0, new Intent(UsbBroadcastReceiver.USB_PERMISSION), 0);
					// and a filter on the permission we ask
					IntentFilter filter = new IntentFilter();
					filter.addAction(UsbBroadcastReceiver.USB_PERMISSION);
					// this broadcast receiver will handle the permission results
					UsbBroadcastReceiver usbReceiver = new UsbBroadcastReceiver(callbackContext, cordova.getActivity());
					cordova.getActivity().registerReceiver(usbReceiver, filter);
					// finally ask for the permission
					manager.requestPermission(device, pendingIntent);
				}
				else {
					// no available drivers
					Log.d(TAG, "No device found!");
					callbackContext.error("No device found!");
				}
			}
		});
	}


	public boolean listDevices() {
		Context currentContext = this.cordova.getActivity().getApplicationContext();
		if (currentContext != null) {
	 		UsbManager usbManager = (UsbManager) currentContext.getSystemService("usb");
	 		for (UsbDevice usbDevice : usbManager.getDeviceList().values()) {
	 			if(!usbDevice.getDeviceName().equals(""))
	 			return true;
			}
		}
	return false;
    }
}
