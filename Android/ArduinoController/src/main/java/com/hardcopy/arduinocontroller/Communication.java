package com.hardcopy.arduinocontroller;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

/**
 * Created by marlon on 14/04/16.
 */
public abstract class Communication {
    public abstract void start(Activity context, InputListener inputListener);
    public abstract void stop();

    public abstract void send(String message);
    public abstract void onActivityResult(int requestCode, int resultCode, Intent data);

    public abstract boolean isConnected();

    protected void buildNotification(Context context, String text, int icon) {
        Log.d("Communication", text);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
        builder.setContentTitle("Communication");
        builder.setContentText(text).setSmallIcon(icon);

        Notification notification = builder.build();
        notification.flags = Notification.FLAG_ONGOING_EVENT;
        NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(0, notification);
    }
}
