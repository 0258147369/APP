package com.example.weddingcountdown;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import java.util.Calendar;

public class CountdownWidgetProvider extends AppWidgetProvider {
    @Override public void onUpdate(Context context, AppWidgetManager manager, int[] ids) {
        updateAll(context);
    }

    public static void updateAll(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        ComponentName component = new ComponentName(context, CountdownWidgetProvider.class);
        int[] ids = manager.getAppWidgetIds(component);
        if (ids == null) return;
        android.content.SharedPreferences prefs = context.getSharedPreferences("wedding_settings", Context.MODE_PRIVATE);
        long target = prefs.getLong("weddingDate", 0L);
        long days = remainingDays(target);
        String count = target == 0L ? "—" : (days == 0 ? "היום!" : (days < 0 ? "עבר" : String.valueOf(days)));
        String theme = prefs.getString("theme", "gold");
        int textColor = "soft".equals(theme) ? 0xFF294032 : 0xFFFFFFFF;
        int background = "soft".equals(theme) ? R.drawable.widget_background_soft
                : ("midnight".equals(theme) ? R.drawable.widget_background_midnight : R.drawable.widget_background);

        for (int id : ids) {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_countdown);
            views.setTextViewText(R.id.widget_days, count);
            views.setInt(R.id.widget_root, "setBackgroundResource", background);
            views.setTextColor(R.id.widget_days, textColor);
            views.setTextColor(R.id.widget_caption, textColor);
            views.setTextColor(R.id.widget_suffix, textColor);
            Intent open = new Intent(context, MainActivity.class);
            PendingIntent pending = PendingIntent.getActivity(context, 8474, open,
                    android.os.Build.VERSION.SDK_INT >= 23
                            ? PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                            : PendingIntent.FLAG_UPDATE_CURRENT);
            views.setOnClickPendingIntent(R.id.widget_days, pending);
            views.setOnClickPendingIntent(R.id.widget_caption, pending);
            manager.updateAppWidget(id, views);
        }
    }

    private static long remainingDays(long target) {
        if (target <= 0L) return -1L;
        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0); today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0); today.set(Calendar.MILLISECOND, 0);
        Calendar wedding = Calendar.getInstance();
        wedding.setTimeInMillis(target);
        wedding.set(Calendar.HOUR_OF_DAY, 0); wedding.set(Calendar.MINUTE, 0);
        wedding.set(Calendar.SECOND, 0); wedding.set(Calendar.MILLISECOND, 0);
        return Math.round((wedding.getTimeInMillis() - today.getTimeInMillis()) / 86400000.0);
    }
}
