package com.todo.app;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.widget.Toast;

public class NotificationHelper {

    public static void scheduleNotification(Context context, int taskId,
                                            String taskTitle, long timeInMillis) {

        // Don't schedule if time is in the past
        if (timeInMillis <= System.currentTimeMillis()) {
            Toast.makeText(context,
                    "Please select a future date and time",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(context, NotificationReceiver.class);
        intent.putExtra(NotificationReceiver.EXTRA_TITLE, taskTitle);
        intent.putExtra(NotificationReceiver.EXTRA_TASK_ID, taskId);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, taskId, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        AlarmManager alarmManager = (AlarmManager)
                context.getSystemService(Context.ALARM_SERVICE);

        if (alarmManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent);
                } else {
                    alarmManager.set(
                            AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent);
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent);
            }
            Toast.makeText(context,
                    "Reminder set! ✓", Toast.LENGTH_SHORT).show();
        }
    }

    public static void cancelNotification(Context context, int taskId) {
        Intent intent = new Intent(context, NotificationReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, taskId, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        AlarmManager alarmManager = (AlarmManager)
                context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);
        }
    }
}