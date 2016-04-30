package com.hardcopy.arduinocontroller;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.AsyncTask;
import android.os.SystemClock;
import android.util.Log;

import com.hoho.android.usbserial.driver.UsbId;
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.HexDump;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by marlon on 14/04/16.
 */
public class UsbCommunication2 extends Communication {
    private Activity activity;
    private static UsbSerialPort sPort = null;
    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();
    private SerialInputOutputManager mSerialIoManager;
    private InputListener inputListener;
    private UsbManager usbManager;
    private final SerialInputOutputManager.Listener mListener =
            new SerialInputOutputManager.Listener() {

        @Override
        public void onRunError(Exception e) {
            Log.d(UsbCommunication2.class.getSimpleName(), "Runner stopped.");
        }

        @Override
        public void onNewData(final byte[] data) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    updateReceivedData(data);
                }
            });
        }
    };

    @Override
    public boolean isConnected() {
        return sPort != null;
    }

    @Override
    public void start(Activity context, InputListener inputListener) {
        usbManager = (UsbManager) activity.getSystemService(Context.USB_SERVICE);
        this.inputListener = inputListener;

        this.sPort = getPortArduino();
        if (this.mListener == null) {
            if (this.sPort != null) {
                UsbDeviceConnection connection = usbManager.openDevice(sPort.getDriver().getDevice());
                if (connection == null) {
                    buildNotification(context, "Dispositivo USB não encontrado.", android.R.drawable.ic_dialog_info);
                    return;
                }
                buildNotification(context, "Conectando USB.", android.R.drawable.ic_dialog_info);

                try {
                    this.sPort.open(connection);
                    this.sPort.setParameters(9600, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);

                    buildNotification(context, "Conectou USB.", android.R.drawable.ic_dialog_info);
                } catch (IOException e) {
                    Log.e(this.getClass().getSimpleName(), "Error setting up device: " + e.getMessage(), e);
                    buildNotification(context, "Erro conectando USB.", android.R.drawable.ic_dialog_info);
                    e.printStackTrace();
                    stop();
                    return;
                }
                onDeviceStateChange();
            }
            else {
                buildNotification(context, "Nenhum dispositivo USB encontrado.", android.R.drawable.ic_dialog_info);
            }
        }
    }

    @Override
    public void stop() {
        try {
            this.sPort.close();
        } catch (IOException e) {
            // Ignore
        }
        this.sPort = null;
    }

    private void stopIoManager() {
        if (mSerialIoManager != null) {
            Log.i(this.getClass().getSimpleName(), "Stopping io manager ..");
            mSerialIoManager.stop();
            mSerialIoManager = null;
        }
    }

    private void startIoManager() {
        if (sPort != null) {
            Log.i(this.getClass().getSimpleName(), "Starting io manager ..");
            mSerialIoManager = new SerialInputOutputManager(sPort, mListener);
            mExecutor.submit(mSerialIoManager);
        }
    }

    private void onDeviceStateChange() {
        stopIoManager();
        startIoManager();
    }

    private void updateReceivedData(byte[] data) {
        final String message = HexDump.dumpHexString(data);
        inputListener.onRead(message);
    }

    @Override
    public void send(String message) {
        try {
            sPort.write(message.getBytes(), 1000);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

    }

    public UsbSerialPort getPortArduino() {
        try {
            return new AsyncTask<Void, Void, UsbSerialPort>() {
                @Override
                protected UsbSerialPort doInBackground(Void... params) {
                    Log.d(UsbCommunication2.class.getSimpleName(), "Refreshing device list ...");
                    SystemClock.sleep(1000);

                    final List<UsbSerialDriver> drivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager);

                    final List<UsbSerialPort> result = new ArrayList<UsbSerialPort>();
                    for (final UsbSerialDriver driver : drivers) {
                        final List<UsbSerialPort> ports = driver.getPorts();
                        Log.d(UsbCommunication2.class.getSimpleName(), String.format("+ %s: %s port%s",
                                driver, Integer.valueOf(ports.size()), ports.size() == 1 ? "" : "s"));
                        if (driver.getDevice().getVendorId() == UsbId.VENDOR_ARDUINO)
                            result.addAll(ports);
                    }

                    return result.get(0);
                }

            }.execute((Void) null).get();
        } catch (Exception e) {
            Log.e(this.getClass().getSimpleName(), "Error getting port: " + e.getMessage(), e);
            e.printStackTrace();
        }
        return null;
    }
}
