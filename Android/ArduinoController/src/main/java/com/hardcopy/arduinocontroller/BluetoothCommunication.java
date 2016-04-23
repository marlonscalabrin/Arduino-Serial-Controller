package com.hardcopy.arduinocontroller;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;

import app.akexorcist.bluetotohspp.library.BluetoothSPP;
import app.akexorcist.bluetotohspp.library.BluetoothState;
import app.akexorcist.bluetotohspp.library.DeviceList;

/**
 * Created by marlon on 14/04/16.
 */
public class BluetoothCommunication extends Communication {

    private BluetoothSPP bt;
    private InputListener inputListener;
    private boolean isConnected = false;
    private String bluetoothName;

    @Override
    public void start(final Activity context, InputListener inputListener) {
        if (bt != null && !isConnected);
            autoConnect();
        this.bt = new BluetoothSPP(context);
        this.inputListener = inputListener;
        this.bt.setOnDataReceivedListener(new BluetoothSPP.OnDataReceivedListener() {
            public void onDataReceived(byte[] data, String message) {
                BluetoothCommunication.this.inputListener.onRead(message);
            }
        });
        this.bt.setBluetoothStateListener(new BluetoothSPP.BluetoothStateListener() {
                                                public void onServiceStateChanged(int state) {
                                                    if (state == BluetoothState.STATE_CONNECTED) {
                                                        buildNotification(context, "Bluetooth Conectado. (s)", android.R.drawable.ic_dialog_info);
                                                        isConnected = true;
                                                    } else if (state == BluetoothState.STATE_LISTEN) {
                                                        autoConnect();
                                                    }
                                                }
                                            }

        );
        this.bt.setBluetoothConnectionListener(new BluetoothSPP.BluetoothConnectionListener() {
            public void onDeviceConnected(String name, String address) {
                buildNotification(context, "Bluetooth conectado. (d)", android.R.drawable.ic_dialog_info);
            }

            public void onDeviceDisconnected() {
                buildNotification(context, "Bluetooth desconectado.", android.R.drawable.ic_dialog_info);
                autoConnect();
            }

            public void onDeviceConnectionFailed() {
                //Toast.makeText(getApplicationContext(), "Falha na conexao", Toast.LENGTH_SHORT).show();
                buildNotification(context, "Falha na conexão com o Bluetooth.", android.R.drawable.ic_dialog_info);
                autoConnect();
            }
        });
        this.bt.setAutoConnectionListener(new BluetoothSPP.AutoConnectionListener() {
            public void onNewConnection(String name, String address) {
                buildNotification(context, "Conectando Bluetooth. (n)", android.R.drawable.ic_dialog_info);
            }

            public void onAutoConnectionStarted() {
                buildNotification(context, "Conectando Bluetooth. (ac)", android.R.drawable.ic_dialog_info);
            }
        });

        if (!isConnected) {
            buildNotification(context, "Procurando Bluetooth.", android.R.drawable.ic_dialog_info);
            this.bt.startDiscovery();
        }

        if(!this.bt.isBluetoothEnabled()) {
            this.bt.enable();
        } else {
            if(!this.bt.isServiceAvailable()) {
                this.bt.setupService();
                this.bt.startService(BluetoothState.DEVICE_OTHER);
            }
        }

        if(!this.bt.isBluetoothAvailable()) {
            // any command for bluetooth is not available
            this.bt.startService(BluetoothState.DEVICE_OTHER);
            Intent intent = new Intent(context.getApplicationContext(), DeviceList.class);
            context.startActivityForResult(intent, BluetoothState.REQUEST_CONNECT_DEVICE);
        }
    }

    @Override
    public boolean isConnected() {
        return isConnected;
    }

    @Override
    public void stop() {
        bt.disconnect();
        bt.stopService();
        bt.cancelDiscovery();
        bt.stopAutoConnect();
    }

    public void autoConnect() {
        isConnected = false;
        if (!this.bt.isBluetoothAvailable()) {
            return;
        }
        try {
            if (!this.bt.isAutoConnecting()) {
                this.bt.startDiscovery();
                this.bt.autoConnect(bluetoothName);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Override
    public void send(String message) {
        this.bt.send(message, false);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == BluetoothState.REQUEST_CONNECT_DEVICE) {
            if(resultCode == Activity.RESULT_OK) {
                String address = data.getExtras().getString(BluetoothState.EXTRA_DEVICE_ADDRESS);
                BluetoothDevice device = bt.getBluetoothAdapter().getRemoteDevice(address);
                bluetoothName = device.getName();
                this.bt.connect(data);
            }
        } else if(requestCode == BluetoothState.REQUEST_ENABLE_BT) {
            if(resultCode == Activity.RESULT_OK) {
                this.bt.setupService();
                this.bt.startService(BluetoothState.DEVICE_ANDROID);
            } else {
                // Do something if user doesn't choose any device (Pressed back)
            }
        }
    }
}
