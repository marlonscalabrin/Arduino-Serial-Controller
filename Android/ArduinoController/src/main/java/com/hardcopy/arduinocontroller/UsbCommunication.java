package com.hardcopy.arduinocontroller;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Handler;
import android.util.Log;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;

public class UsbCommunication extends Communication {
	public static final String tag = "SerialConnector";

	private Context mContext;
	private ReadListener readListener;
	
	private SerialMonitorThread mSerialThread;
	
	private UsbSerialDriver mDriver;
	private UsbSerialPort mPort;
	
	public static final int TARGET_VENDOR_ID = 9025;	// Arduino
	public static final int BAUD_RATE = 9600;

	public boolean isConnected() {
		return mPort != null;
	}

	@Override
	public void start(Activity context, ReadListener readListener) {
		mContext = context;
		this.readListener = readListener;
		UsbManager manager = (UsbManager) mContext.getSystemService(Context.USB_SERVICE);

		try {
			List<UsbSerialDriver> availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(manager);
			if (availableDrivers.isEmpty()) {
				readListener.onError("Error: There is no available device. \n");
				return;
			}
		
			mDriver = availableDrivers.get(0);
			if(mDriver == null) {
				readListener.onError( "Error: Driver is Null \n");
				return;
			}
		} catch (NullPointerException e) {
			e.printStackTrace();
			return;
		}
		
		// Report to UI
		StringBuilder sb = new StringBuilder();
		UsbDevice device = mDriver.getDevice();
		sb.append(" DName : ").append(device.getDeviceName()).append("\n")
			.append(" DID : ").append(device.getDeviceId()).append("\n")
			.append(" VID : ").append(device.getVendorId()).append("\n")
			.append(" PID : ").append(device.getProductId()).append("\n")
			.append(" IF Count : ").append(device.getInterfaceCount()).append("\n");
		readListener.onError(sb.toString());
		
		UsbDeviceConnection connection = manager.openDevice(device);
		if (connection == null) {
			readListener.onError("Error: Cannot connect to device. \n");
			return;
		}
		
		// Read some data! Most have just one port (port 0).
		mPort = mDriver.getPorts().get(0);
		if(mPort == null) {
			readListener.onError("Error: Cannot get port. \n");
			return;
		}
		
		try {
			mPort.open(connection);
			mPort.setParameters(BAUD_RATE, 8, 1, 0);
		} catch (IOException e) {
			// Deal with error.
			readListener.onError("Error: Cannot open port \n" + e.toString() + "\n");
		} finally {
		}
		
		// Everything is fine. Start serial monitoring thread.
		startThread();
	}	// End of initialize()
	
	public void finalize() {
		try {
			mDriver = null;
			stop();
			
			mPort.close();
			mPort = null;
		} catch(Exception ex) {
			readListener.onError("Error: Cannot finalize serial connector \n" + ex.toString() + "\n");
		}
	}



	/*****************************************************
	*	public methods
	******************************************************/
	// send string to remote
	public void send(String message) {

		if(mPort != null && message != null) {
			try {
				mPort.write(message.getBytes(), message.length());		// Send to remote device
			}
			catch(IOException e) {
				readListener.onError("Failed in sending command. : IO Exception \n");
			}
		}
	}


	/*****************************************************
	*	private methods
	******************************************************/
	// start thread
	private void startThread() {
		Log.d(tag, "Start serial monitoring thread");
		readListener.onError("Start serial monitoring thread \n");
		if(mSerialThread == null) {
			mSerialThread = new SerialMonitorThread();
			mSerialThread.start();
		}	
	}
	// stop thread

	@Override
	public void stop() {
		if(mSerialThread != null && mSerialThread.isAlive())
			mSerialThread.interrupt();
		if(mSerialThread != null) {
			mSerialThread.setKillSign(true);
			mSerialThread = null;
		}
	}


	/*****************************************************
	*	Sub classes, Handler, Listener
	******************************************************/

	public class SerialMonitorThread extends Thread {
		// Thread status
		private boolean mKillSign = false;
		private SerialCommand mCmd = new SerialCommand();


		private void initializeThread() {
			// This code will be executed only once.
		}

		private void finalizeThread() {
		}

		// stop this thread
		public void setKillSign(boolean isTrue) {
			mKillSign = isTrue;
		}

		/**
		*	Main loop
		**/
		@Override
		public void run()
		{
			byte buffer[] = new byte[128];

			while(!Thread.interrupted())
			{
				if(mPort != null) {
					Arrays.fill(buffer, (byte)0x00);

					try {
						// Read received buffer
						int numBytesRead = mPort.read(buffer, 1000);
						if(numBytesRead > 0) {
							Log.d(tag, "run : read bytes = " + numBytesRead);

							// Print message length

							// Extract data from buffer
							for(int i=0; i<numBytesRead; i++) {
								char c = (char)buffer[i];
								if(c == 'z') {
									// This is end signal. Send collected result to UI
									if(mCmd.mStringBuffer != null && mCmd.mStringBuffer.length() < 20) {
										readListener.onRead( mCmd.toString());
									}
								} else {
									mCmd.addChar(c);
								}
							}
						} // End of if(numBytesRead > 0)
					}
					catch (IOException e) {
						Log.d(tag, "IOException - mDriver.read");
						readListener.onError("Error # run: " + e.toString() + "\n");
						mKillSign = true;
					}
				}

				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
					break;
				}

				if(mKillSign)
					break;

			}	// End of while() loop

			// Finalize
			finalizeThread();

		}	// End of run()


	}	// End of SerialMonitorThread


	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {

	}
}
