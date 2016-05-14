package com.hardcopy.arduinocontroller;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * Created by marlon on 14/05/16.
 */
public class SocketCommunication extends Communication {

    private final String addr;
    private final int port;

    private Socket socket = null;
    private InputStream inputStream = null;
    private ByteArrayOutputStream byteArrayOutputStream = null;

    public SocketCommunication(String addr, int port) {
        this.addr = addr;
        this.port = port;
    }

    @Override
    public void start(Activity context, final InputListener inputListener) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... arg0) {
                String response =  "";

                try {
                    socket = new Socket(addr, port);

                    byteArrayOutputStream = new ByteArrayOutputStream(1024);
                    byte[] buffer = new byte[1024];

                    int bytesRead;
                    inputStream = socket.getInputStream();

                    while (this.getStatus() == Status.RUNNING) {
                        response =  "";
                        while (inputStream.available() > 0 && (bytesRead = inputStream.read(buffer)) != -1) {
                            //byteArrayOutputStream.write(buffer, 0, bytesRead); // echo
                            response += new String(buffer);
                        }
                        inputListener.onRead(response);
                    }

                } catch (UnknownHostException e) {
                    e.printStackTrace();
                    response = "UnknownHostException: " + e.toString();
                    inputListener.onError(response);
                } catch (IOException e) {
                    e.printStackTrace();
                    response = "IOException: " + e.toString();
                    inputListener.onError(response);
                } finally{
                    stop();
                }
                return null;
            }
        }.execute();
    }

    @Override
    public void stop() {
        try {
            inputStream.close();
            byteArrayOutputStream.close();
            socket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void send(String message) {
        try {
            byteArrayOutputStream.write(message.getBytes(), 0, message.length());
            byteArrayOutputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

    }

    @Override
    public boolean isConnected() {
        return socket.isConnected();
    }
}
