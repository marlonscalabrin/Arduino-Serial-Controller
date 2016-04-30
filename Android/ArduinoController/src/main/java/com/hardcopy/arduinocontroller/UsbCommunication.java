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
import android.util.Log;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;

public class UsbCommunication extends Communication {
	public static final String tag = "SerialConnector";
	private Context mContext;

	private UsbSerialDriver mDriver;
	private UsbSerialPort mPort;
	private InputListener inputListener;
	private InputListenerThread listenerThread;

	public static final int BAUD_RATE = 9600;

	public boolean isConnected() {
		return mPort != null;
	}

	@Override
	public void start(Activity context, InputListener inputListener) {
		mContext = context;
		this.inputListener = inputListener;
		UsbManager manager = (UsbManager) mContext.getSystemService(Context.USB_SERVICE);

		try {
			List<UsbSerialDriver> availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(manager);
			if (availableDrivers.isEmpty()) {
				inputListener.onError("Error: There is no available device. \n");
				return;
			}
		
			mDriver = availableDrivers.get(0);
			if(mDriver == null) {
				inputListener.onError( "Error: Driver is Null \n");
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
		inputListener.onError(sb.toString());
		
		UsbDeviceConnection connection = manager.openDevice(device);
		if (connection == null) {
			inputListener.onError("Error: Cannot connect to device. \n");
			return;
		}
		
		// Read some data! Most have just one port (port 0).
		mPort = mDriver.getPorts().get(0);
		if(mPort == null) {
			inputListener.onError("Error: Cannot get port. \n");
			return;
		}
		
		try {
			mPort.open(connection);
			mPort.setParameters(BAUD_RATE, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
		} catch (IOException e) {
			// Deal with error.
			inputListener.onError("Error: Cannot open port \n" + e.toString() + "\n");
		} finally {
		}
		
		// Everything is fine. Start serial monitoring thread.
		startThread();
	}
	
	public void finalize() {
		try {
			mDriver = null;
			stop();
			
			mPort.close();
			mPort = null;
		} catch(Exception ex) {
			inputListener.onError("Error: Cannot finalize serial connector \n" + ex.toString() + "\n");
		}
	}

	/** send string to remote **/
	public void send(String message) {

		if(mPort != null && message != null) {
			try {
				mPort.write(message.getBytes(), message.length());		// Send to remote device
			}
			catch(IOException e) {
				inputListener.onError("Failed in sending command. : IO Exception \n");
			}
		}
	}

	/** start thread **/
	private void startThread() {
		Log.d(tag, "Start serial monitoring thread");
		inputListener.onError("Start serial monitoring thread \n");
		if(listenerThread == null || listenerThread.isInterrupted()) {
			listenerThread = new InputListenerThread();
			listenerThread.start();
		}	
	}

	/** stop thread **/
	@Override
	public void stop() {
		if(listenerThread != null && listenerThread.isAlive())
			listenerThread.interrupt();
		if(listenerThread != null) {
			listenerThread.setKillSign(true);
			listenerThread = null;
		}
	}


	/*****************************************************
	*	Sub classes, Handler, Listener
	******************************************************/

	public class InputListenerThread extends Thread {
		// Thread status
		private boolean mKillSign = false;
		private Message message = new Message();


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
							for(int i = 0; i < numBytesRead; i++) {
								char c = (char)buffer[i];
								if(c == '\n') {
									// This is end signal. Send collected result to UI
									inputListener.onRead(message.toString());
								}
								message.addChar(c);
							}
						} // End of if(numBytesRead > 0)
					}
					catch (IOException e) {
						Log.d(tag, "IOException - mDriver.read");
						inputListener.onError("Error # run: " + e.toString() + "\n");
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
