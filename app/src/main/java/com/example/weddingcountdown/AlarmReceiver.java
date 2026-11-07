package com.example.weddingcountdown;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.widget.RemoteViews;

import java.util.Calendar;
import java.util.Locale;

public class AlarmReceiver extends BroadcastReceiver {
    private static final String CHANNEL_ID = "wedding_countdown_daily";
    private static final int ALARM_ID = 8471;

    @Override public void onReceive(Context context, Intent intent) {
        android.content.SharedPreferences prefs = context.getSharedPreferences("wedding_settings", Context.MODE_PRIVATE);
        if (!prefs.getBoolean("muted", false)) {
            long remaining = remainingDays(prefs.getLong("weddingDate", 0L));
            boolean daily = prefs.getBoolean("daily", true);
            boolean milestones = prefs.getBoolean("milestones", true);
            boolean isMilestone = remaining == 100 || remaining == 50 || remaining == 30 || remaining == 14 || remaining == 7
                    || remaining == 3 || remaining == 1 || remaining == 0;
            if (remaining >= 0 && ((daily && remaining >= 0) || (milestones && isMilestone))) {
                showNotification(context, remaining);
            }
        }
        scheduleNext(context);
        CountdownWidgetProvider.updateAll(context);
    }

    public static void scheduleNext(Context context) {
        android.content.SharedPreferences prefs = context.getSharedPreferences("wedding_settings", Context.MODE_PRIVATE);
        AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (manager == null) return;
        Intent intent = new Intent(context, AlarmReceiver.class);
        PendingIntent pending = PendingIntent.getBroadcast(context, ALARM_ID, intent,
                Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE : PendingIntent.FLAG_UPDATE_CURRENT);
        manager.cancel(pending);

        if (prefs.getBoolean("muted", false)
                || (!prefs.getBoolean("daily", true) && !prefs.getBoolean("milestones", true))) return;

        long target = prefs.getLong("weddingDate", 0L);
        if (target == 0L || remainingDays(target) < 0) return;

        String[] hm = prefs.getString("alertTime", "09:00").split(":");
        int hour = 9, minute = 0;
        try { hour = Integer.parseInt(hm[0]); minute = Integer.parseInt(hm[1]); } catch (Exception ignored) { }

        Calendar now = Calendar.getInstance();
        Calendar candidate = (Calendar) now.clone();
        candidate.set(Calendar.HOUR_OF_DAY, hour);
        candidate.set(Calendar.MINUTE, minute);
        candidate.set(Calendar.SECOND, 0);
        candidate.set(Calendar.MILLISECOND, 0);
        if (candidate.getTimeInMillis() <= now.getTimeInMillis()) candidate.add(Calendar.DAY_OF_YEAR, 1);

        if (!prefs.getBoolean("daily", true)) {
            long nextMilestone = nextMilestoneTime(target, candidate, hour, minute);
            if (nextMilestone <= 0L) return;
            candidate.setTimeInMillis(nextMilestone);
        }

        long fireAt = candidate.getTimeInMillis();
        if (Build.VERSION.SDK_INT >= 23) {
            manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, fireAt, pending);
        } else if (Build.VERSION.SDK_INT >= 19) {
            manager.setExact(AlarmManager.RTC_WAKEUP, fireAt, pending);
        } else {
            manager.set(AlarmManager.RTC_WAKEUP, fireAt, pending);
        }
    }

    private static long nextMilestoneTime(long target, Calendar after, int hour, int minute) {
        int[] milestones = new int[]{100, 50, 30, 14, 7, 3, 1, 0};
        Calendar wedding = Calendar.getInstance();
        wedding.setTimeInMillis(target);
        wedding.set(Calendar.HOUR_OF_DAY, hour);
        wedding.set(Calendar.MINUTE, minute);
        wedding.set(Calendar.SECOND, 0);
        wedding.set(Calendar.MILLISECOND, 0);
        for (int days : milestones) {
            Calendar at = (Calendar) wedding.clone();
            at.add(Calendar.DAY_OF_YEAR, -days);
            if (at.getTimeInMillis() >= after.getTimeInMillis()) return at.getTimeInMillis();
        }
        return -1L;
    }

    private static long remainingDays(long target) {
        if (target <= 0L) return -1L;
        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0); today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0); today.set(Calendar.MILLISECOND, 0);
        Calendar event = Calendar.getInstance();
        event.setTimeInMillis(target);
        event.set(Calendar.HOUR_OF_DAY, 0); event.set(Calendar.MINUTE, 0);
        event.set(Calendar.SECOND, 0); event.set(Calendar.MILLISECOND, 0);
        return Math.round((event.getTimeInMillis() - today.getTimeInMillis()) / 86400000.0);
    }

    private void showNotification(Context context, long days) {
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) return;
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "ספירה לאחור", NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("תזכורות לגבי מספר הימים שנותרו");
            manager.createNotificationChannel(channel);
        }

        Intent open = new Intent(context, MainActivity.class);
        open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent content = PendingIntent.getActivity(context, 8472, open,
                Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE : PendingIntent.FLAG_UPDATE_CURRENT);

        String message = days == 0 ? "היום זה היום הגדול!" : "נשארו " + days + " ימים לחתונה";
        Notification.Builder builder = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(context, CHANNEL_ID)
                : new Notification.Builder(context);
        Notification notification = builder
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("עוד רגע אחד גדול")
                .setContentText(message)
                .setContentIntent(content)
                .setAutoCancel(true)
                .setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE)
                .build();
        manager.notify(8473, notification);
    }
}
