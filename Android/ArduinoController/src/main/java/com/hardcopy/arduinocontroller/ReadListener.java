package com.hardcopy.arduinocontroller;

/**
 * Created by marlon on 14/04/16.
 */
public interface ReadListener {
    public void onRead(String message);
    public void onError(String error);
}
