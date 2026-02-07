package com.airplanescheduler;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;
import androidx.core.app.NotificationCompat;

public class ScheduleReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "airplane_mode_channel";
    private static final String TAG = "ScheduleReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        boolean turnOn = intent.getBooleanExtra("turn_on", true);
        String scheduleId = intent.getStringExtra("schedule_id");
        
        Log.d(TAG, "Received alarm - Turn ON: " + turnOn + ", Schedule: " + scheduleId);
        
        boolean success = toggleAirplaneMode(context, turnOn);
        showNotification(context, turnOn, success);
    }

    private boolean toggleAirplaneMode(Context context, boolean turnOn) {
        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
                Settings.System.putInt(context.getContentResolver(),
                        Settings.System.AIRPLANE_MODE_ON, turnOn ? 1 : 0);
                Intent intent = new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED);
                intent.putExtra("state", turnOn);
                context.sendBroadcast(intent);
                return true;
            } else {
                Log.w(TAG, "Cannot toggle airplane mode on API " + Build.VERSION.SDK_INT);
                return false;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error toggling airplane mode", e);
            return false;
        }
    }

    private void showNotification(Context context, boolean turnOn, boolean success) {
        NotificationManager notificationManager = 
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Airplane Mode Scheduler",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Notifications for airplane mode schedule");
            notificationManager.createNotificationChannel(channel);
        }

        String title = success ? 
                (turnOn ? "Airplane Mode ON" : "Airplane Mode OFF") :
                (turnOn ? "Turn ON Airplane Mode?" : "Turn OFF Airplane Mode?");

        String message = success ?
                "Airplane mode has been " + (turnOn ? "enabled" : "disabled") + " automatically." :
                "Please " + (turnOn ? "enable" : "disable") + " airplane mode manually.";

        Intent intent = new Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, intent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        notificationManager.notify((int) System.currentTimeMillis(), builder.build());
    }
}
