package com.example.weddingcountdown;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final int PICK_IMAGE = 6104;
    private static final String PREFS = "wedding_settings";
    private android.content.SharedPreferences prefs;
    private LinearLayout page;
    private int currentScreen = 0;
    private int pendingImageAction = 0; // 0 = cover, 1 = gallery
    private int ink = Color.rgb(239, 237, 232);
    private int muted = Color.rgb(151, 164, 171);
    private int rose = Color.rgb(216, 183, 124);

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        if (android.os.Build.VERSION.SDK_INT >= 21) {
            getWindow().setStatusBarColor(Color.rgb(12, 17, 22));
            getWindow().setNavigationBarColor(Color.rgb(17, 24, 30));
        }
        AlarmReceiver.scheduleNext(this);
        CountdownWidgetProvider.updateAll(this);
        render();
    }

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private GradientDrawable shape(int color, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radius));
        return d;
    }

    private GradientDrawable gradient(int start, int end, int radius) {
        GradientDrawable d = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{start, end});
        d.setCornerRadius(dp(radius));
        return d;
    }

    private TextView label(String value, int size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        t.setGravity(Gravity.CENTER);
        t.setTypeface(Typeface.create("sans-serif", bold ? Typeface.BOLD : Typeface.NORMAL));
        t.setIncludeFontPadding(true);
        return t;
    }

    private TextView rightLabel(String value, int size, int color, boolean bold) {
        TextView t = label(value, size, color, bold);
        t.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        return t;
    }

    private LinearLayout column() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    private LinearLayout.LayoutParams lp(int width, int height) {
        return new LinearLayout.LayoutParams(width < 0 ? width : dp(width), height < 0 ? height : dp(height));
    }

    private void space(LinearLayout parent, int height) {
        parent.addView(new View(this), lp(1, height));
    }

    private TextView action(String text, int background, int foreground, final Runnable task) {
        TextView b = label(text, 15, foreground, true);
        b.setPadding(dp(16), dp(13), dp(16), dp(13));
        b.setBackgroundDrawable(shape(background, 15));
        b.setClickable(true);
        b.setFocusable(true);
        b.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { task.run(); }
        });
        return b;
    }

    private void render() {
        LinearLayout shell = column();
         shell.setBackgroundColor(Color.rgb(12, 17, 22));
        page = shell;
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout.LayoutParams contentParams = new LinearLayout.LayoutParams(-1, 0, 1);
        shell.addView(scroll, contentParams);

        LinearLayout content = column();
        content.setPadding(dp(20), dp(22), dp(20), dp(24));
        scroll.addView(content);

        if (currentScreen == 0) buildHome(content);
        else if (currentScreen == 1) buildGallery(content);
        else buildSettings(content);

        shell.addView(buildNavigation(), lp(-1, 66));
        setContentView(shell);
    }

    private LinearLayout buildNavigation() {
        LinearLayout nav = new LinearLayout(this);
        nav.setGravity(Gravity.CENTER);
        nav.setPadding(dp(10), dp(8), dp(10), dp(8));
        nav.setBackgroundColor(Color.rgb(17, 24, 30));
        if (android.os.Build.VERSION.SDK_INT >= 21) nav.setElevation(dp(10));
        String[] names = {"⌂  בית", "▧  גלריה", "⚙  הגדרות"};
        for (int i = 0; i < names.length; i++) {
            final int selected = i;
            TextView item = label(names[i], 13, currentScreen == i ? rose : muted, currentScreen == i);
            item.setClickable(true);
            item.setFocusable(true);
            item.setPadding(dp(7), dp(10), dp(7), dp(10));
            if (currentScreen == i) item.setBackgroundDrawable(shape(Color.rgb(35, 44, 49), 14));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, -1, 1);
            params.setMargins(dp(4), 0, dp(4), 0);
            nav.addView(item, params);
            item.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { currentScreen = selected; render(); }
            });
        }
        return nav;
    }

    private void buildHome(LinearLayout content) {
        TextView eyebrow = rightLabel("YOUR COUNTDOWN  /  הספירה שלכם", 11, rose, true);
        content.addView(eyebrow, lp(-1, 26));

        TextView title = rightLabel("הרגע שלכם מתקרב.", 29, ink, true);
        content.addView(title, lp(-1, 48));

        TextView subtitle = rightLabel("כל יום הוא עוד צעד אל היום הגדול", 14, muted, false);
        content.addView(subtitle, lp(-1, 27));
        space(content, 22);

        final long target = prefs.getLong("weddingDate", 0L);
        long days = remainingDays();

        FrameLayout hero = new FrameLayout(this);
        hero.setBackgroundDrawable(gradient(Color.rgb(39, 56, 63), Color.rgb(18, 28, 34), 28));
        LinearLayout.LayoutParams heroParams = lp(-1, 370);
        heroParams.bottomMargin = dp(18);
        content.addView(hero, heroParams);

        String cover = prefs.getString("cover", "");
        if (cover.length() > 0) {
            File f = new File(getFilesDir(), cover);
            if (f.exists()) {
                ImageView image = new ImageView(this);
                image.setScaleType(ImageView.ScaleType.CENTER_CROP);
                image.setImageBitmap(loadBitmap(f.getAbsolutePath(), 1200));
                hero.addView(image, new FrameLayout.LayoutParams(-1, -1));
                View shade = new View(this);
                shade.setBackgroundDrawable(gradient(Color.argb(95, 7, 13, 17), Color.argb(220, 8, 14, 18), 28));
                hero.addView(shade, new FrameLayout.LayoutParams(-1, -1));
            }
        }

        LinearLayout heroContent = column();
        heroContent.setGravity(Gravity.CENTER);
        heroContent.setPadding(dp(22), dp(22), dp(22), dp(22));
        hero.addView(heroContent, new FrameLayout.LayoutParams(-1, -1));

        TextView tag = label("UNTIL THE BIG DAY", 10, rose, true);
        if (android.os.Build.VERSION.SDK_INT >= 21) tag.setLetterSpacing(0.16f);
        heroContent.addView(tag, lp(-1, 26));

        String number = target == 0 ? "—" : (days <= 0 ? "היום" : String.valueOf(days));
        TextView big = label(number, days > 0 ? 88 : 55, Color.WHITE, true);
        big.setShadowLayer(14, 0, 3, Color.argb(100, 0, 0, 0));
        heroContent.addView(big, lp(-1, 125));

        TextView desc = label(target == 0 ? "התאריך שלכם עוד לא הוגדר" :
                (days <= 0 ? "זה היום שלכם. תיהנו מכל רגע." : "ימים עד לרגע המיוחד"), 18, Color.rgb(242, 239, 231), true);
        heroContent.addView(desc, lp(-1, 38));

        View line = new View(this);
        line.setBackgroundColor(Color.argb(90, 225, 218, 200));
        LinearLayout.LayoutParams lineParams = lp(56, 1);
        lineParams.topMargin = dp(10);
        lineParams.bottomMargin = dp(10);
        heroContent.addView(line, lineParams);

        String dateText = target == 0 ? "מגדירים תאריך פעם אחת בהגדרות" :
                new SimpleDateFormat("EEEE  ·  dd.MM.yyyy", new Locale("he")).format(new Date(target));
        TextView date = label(dateText, 14, Color.rgb(190, 196, 194), false);
        heroContent.addView(date, lp(-1, 30));

        if (target == 0) {
            space(heroContent, 12);
            heroContent.addView(label("אפשר להגדיר תאריך במסך ההגדרות", 13, Color.rgb(185, 191, 188), false), lp(-1, 36));
        }

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.CENTER);
        TextView photoAction = action("＋  תמונת רקע", Color.rgb(216, 183, 124), Color.rgb(24, 29, 32), new Runnable() {
            @Override public void run() { pickImage(0); }
        });
        actions.addView(photoAction, new LinearLayout.LayoutParams(0, dp(54), 1));
        View gap = new View(this);
        actions.addView(gap, lp(10, 1));
        TextView galleryAction = action("הגלריה שלי  ›", Color.rgb(35, 45, 52), ink, new Runnable() {
            @Override public void run() { currentScreen = 1; render(); }
        });
        actions.addView(galleryAction, new LinearLayout.LayoutParams(0, dp(54), 1));
        content.addView(actions, lp(-1, 56));

        space(content, 18);
        LinearLayout note = card();
        note.setOrientation(LinearLayout.HORIZONTAL);
        note.setGravity(Gravity.CENTER_VERTICAL);
        TextView dot = label("✦", 22, rose, true);
        LinearLayout.LayoutParams dotParams = lp(34, 42);
        note.addView(dot, dotParams);
        LinearLayout noteText = column();
        noteText.addView(rightLabel("הרגעים שלכם, במקום אחד", 15, ink, true), lp(-1, 27));
        noteText.addView(rightLabel("התמונות וההגדרות נשמרות במכשיר בלבד", 12, muted, false), lp(-1, 24));
        note.addView(noteText, new LinearLayout.LayoutParams(0, -2, 1));
        content.addView(note, lp(-1, 82));
    }

    private LinearLayout card() {
        LinearLayout c = column();
        c.setPadding(dp(17), dp(15), dp(17), dp(15));
        c.setBackgroundDrawable(shape(Color.rgb(25, 34, 41), 20));
        if (android.os.Build.VERSION.SDK_INT >= 21) c.setElevation(dp(2));
        return c;
    }

    private void buildGallery(LinearLayout content) {
        content.addView(rightLabel("הגלריה", 29, ink, true), lp(-1, 48));
        content.addView(rightLabel("כל התמונות ששמרתם במקום אחד", 14, muted, false), lp(-1, 28));
        space(content, 16);
        content.addView(action("＋  הוספת תמונה לגלריה", rose, Color.WHITE, new Runnable() {
            @Override public void run() { pickImage(1); }
        }), lp(-1, 52));
        space(content, 14);

        String cover = prefs.getString("cover", "");
        if (cover.length() > 0) {
            File f = new File(getFilesDir(), cover);
            if (f.exists()) {
                content.addView(rightLabel("תמונת הרקע", 17, ink, true), lp(-1, 32));
                addPhoto(content, f, false);
                space(content, 14);
            }
        }

        String list = prefs.getString("gallery", "");
        String[] names = list.length() == 0 ? new String[0] : list.split("\\|");
        if (names.length == 0) {
            LinearLayout empty = card();
            empty.addView(label("כאן יופיעו התמונות שתוסיפו", 15, muted, false), lp(-1, 70));
            content.addView(empty, lp(-1, 100));
        } else {
            content.addView(rightLabel("הגלריה שלכם", 17, ink, true), lp(-1, 32));
            for (String name : names) {
                File f = new File(getFilesDir(), name);
                if (f.exists()) {
                    addPhoto(content, f, true);
                    space(content, 12);
                }
            }
        }
    }

    private void addPhoto(LinearLayout content, final File file, final boolean fromGallery) {
        ImageView image = new ImageView(this);
        image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        image.setImageBitmap(loadBitmap(file.getAbsolutePath(), 900));
        image.setBackgroundDrawable(shape(Color.rgb(25, 34, 41), 18));
        LinearLayout.LayoutParams p = lp(-1, 190);
        content.addView(image, p);
        image.setClickable(true);
        image.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                final String[] options = fromGallery
                        ? new String[]{"הגדרה כתמונת רקע", "הסרה מהגלריה", "ביטול"}
                        : new String[]{"החלפת תמונת הרקע", "ביטול"};
                new AlertDialog.Builder(MainActivity.this).setTitle("אפשרויות לתמונה")
                        .setItems(options, (dialog, which) -> {
                            if (fromGallery && which == 0) {
                                prefs.edit().putString("cover", file.getName()).apply();
                                currentScreen = 0; render();
                            } else if (fromGallery && which == 1) {
                                removeFromGallery(file.getName());
                                render();
                            } else if (!fromGallery && which == 0) {
                                pickImage(0);
                            }
                        }).show();
            }
        });
    }

    private void removeFromGallery(String name) {
        ArrayList<String> items = galleryNames();
        items.remove(name);
        prefs.edit().putString("gallery", join(items)).apply();
        if (!name.equals(prefs.getString("cover", ""))) new File(getFilesDir(), name).delete();
        Toast.makeText(this, "התמונה הוסרה", Toast.LENGTH_SHORT).show();
    }

    private ArrayList<String> galleryNames() {
        ArrayList<String> items = new ArrayList<String>();
        String raw = prefs.getString("gallery", "");
        if (raw.length() > 0) {
            String[] split = raw.split("\\|");
            for (String s : split) if (s.length() > 0) items.add(s);
        }
        return items;
    }

    private String join(ArrayList<String> items) {
        StringBuilder b = new StringBuilder();
        for (String s : items) {
            if (b.length() > 0) b.append("|");
            b.append(s);
        }
        return b.toString();
    }

    private void buildSettings(LinearLayout content) {
        content.addView(rightLabel("הגדרות", 27, ink, true), lp(-1, 44));
        content.addView(rightLabel("כמה פשוט. בדיוק כמו שצריך.", 14, muted, false), lp(-1, 28));
        space(content, 16);

        LinearLayout dateCard = card();
        dateCard.addView(rightLabel("תאריך החתונה", 17, ink, true), lp(-1, 30));
        long date = prefs.getLong("weddingDate", 0L);
        String dateText = date == 0 ? "לא נבחר תאריך" : new SimpleDateFormat("EEEE, dd.MM.yyyy", new Locale("he")).format(new Date(date));
        dateCard.addView(rightLabel(dateText, 14, muted, false), lp(-1, 28));
        space(dateCard, 8);
        dateCard.addView(action("בחירת תאריך", rose, Color.WHITE, new Runnable() {
            @Override public void run() { chooseDate(); }
        }), lp(-1, 48));
        content.addView(dateCard, lp(-1, -2));
        space(content, 14);

        LinearLayout alerts = card();
        alerts.addView(rightLabel("התראות", 18, ink, true), lp(-1, 34));
        Switch daily = new Switch(this);
        daily.setText("התראה יומית");
        daily.setTextSize(15);
        daily.setTextColor(ink);
        daily.setChecked(prefs.getBoolean("daily", true));
        daily.setOnCheckedChangeListener((button, checked) -> {
            prefs.edit().putBoolean("daily", checked).apply();
            AlarmReceiver.scheduleNext(MainActivity.this);
        });
        alerts.addView(daily, lp(-1, 48));

        Switch milestones = new Switch(this);
        milestones.setText("התראות על ציוני דרך");
        milestones.setTextSize(15);
        milestones.setTextColor(ink);
        milestones.setChecked(prefs.getBoolean("milestones", true));
        milestones.setOnCheckedChangeListener((button, checked) -> {
            prefs.edit().putBoolean("milestones", checked).apply();
            AlarmReceiver.scheduleNext(MainActivity.this);
        });
        alerts.addView(milestones, lp(-1, 48));

        Switch mutedSwitch = new Switch(this);
        mutedSwitch.setText("השתקת כל ההתראות");
        mutedSwitch.setTextSize(15);
        mutedSwitch.setTextColor(ink);
        mutedSwitch.setChecked(prefs.getBoolean("muted", false));
        mutedSwitch.setOnCheckedChangeListener((button, checked) -> {
            prefs.edit().putBoolean("muted", checked).apply();
            AlarmReceiver.scheduleNext(MainActivity.this);
        });
        alerts.addView(mutedSwitch, lp(-1, 48));

        TextView time = action("שעת התראה: " + prefs.getString("alertTime", "09:00"), Color.rgb(34, 44, 52), rose, new Runnable() {
            @Override public void run() { chooseTime(); }
        });
        LinearLayout.LayoutParams timeParams = lp(-1, 48);
        timeParams.topMargin = dp(8);
        alerts.addView(time, timeParams);
        content.addView(alerts, lp(-1, -2));
        space(content, 14);

        LinearLayout widget = card();
        widget.addView(rightLabel("ווידג׳ט למסך הבית", 17, ink, true), lp(-1, 32));
        widget.addView(rightLabel("אפשר להוסיף למסך הבית ווידג׳ט קטן שמציג את מספר הימים שנותרו. הוסיפו אותו דרך תפריט הווידג׳טים של המכשיר.", 14, muted, false), lp(-1, 66));
        content.addView(widget, lp(-1, -2));
        space(content, 14);

        LinearLayout offline = card();
        offline.addView(rightLabel("פרטיות ושמירת מידע", 17, ink, true), lp(-1, 30));
        offline.addView(rightLabel("התאריך והתמונות נשמרים במכשיר בלבד. האפליקציה פועלת גם ללא חיבור לאינטרנט.", 14, muted, false), lp(-1, 54));
        content.addView(offline, lp(-1, -2));
    }

    private void chooseDate() {
        Calendar c = Calendar.getInstance();
        long stored = prefs.getLong("weddingDate", 0L);
        if (stored > 0) c.setTimeInMillis(stored);
        DatePickerDialog dlg = new DatePickerDialog(this, (view, year, month, day) -> {
            Calendar selected = Calendar.getInstance();
            selected.set(year, month, day, 0, 0, 0);
            selected.set(Calendar.MILLISECOND, 0);
            prefs.edit().putLong("weddingDate", selected.getTimeInMillis()).apply();
            AlarmReceiver.scheduleNext(MainActivity.this);
            CountdownWidgetProvider.updateAll(MainActivity.this);
            render();
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
        dlg.show();
    }

    private void chooseTime() {
        String[] parts = prefs.getString("alertTime", "09:00").split(":");
        int hour = Integer.parseInt(parts[0]);
        int minute = Integer.parseInt(parts[1]);
        new TimePickerDialog(this, (view, h, m) -> {
            String value = String.format(Locale.US, "%02d:%02d", h, m);
            prefs.edit().putString("alertTime", value).apply();
            AlarmReceiver.scheduleNext(MainActivity.this);
            render();
        }, hour, minute, true).show();
    }

    private void pickImage(int action) {
        pendingImageAction = action;
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        try {
            startActivityForResult(Intent.createChooser(intent, "בחירת תמונה"), PICK_IMAGE);
        } catch (Exception e) {
            Toast.makeText(this, "לא נמצאה אפליקציה לבחירת תמונה", Toast.LENGTH_LONG).show();
        }
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != PICK_IMAGE || resultCode != RESULT_OK || data == null || data.getData() == null) return;
        try {
            Uri uri = data.getData();
            InputStream in = getContentResolver().openInputStream(uri);
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int n;
            while ((n = in.read(buffer)) > 0) bytes.write(buffer, 0, n);
            in.close();
            byte[] raw = bytes.toByteArray();
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(raw, 0, raw.length, bounds);
            int sample = 1;
            while (bounds.outWidth / sample > 1600 || bounds.outHeight / sample > 1600) sample *= 2;
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inSampleSize = sample;
            Bitmap bitmap = BitmapFactory.decodeByteArray(raw, 0, raw.length, opts);
            if (bitmap == null) throw new Exception("Invalid image");
            String name = "photo_" + System.currentTimeMillis() + ".jpg";
            FileOutputStream out = new FileOutputStream(new File(getFilesDir(), name));
            bitmap.compress(Bitmap.CompressFormat.JPEG, 88, out);
            out.flush(); out.close(); bitmap.recycle();

            if (pendingImageAction == 0) {
                String old = prefs.getString("cover", "");
                prefs.edit().putString("cover", name).apply();
                if (old.length() > 0 && !old.equals(name) && !galleryNames().contains(old)) new File(getFilesDir(), old).delete();
                currentScreen = 0;
                Toast.makeText(this, "תמונת הרקע עודכנה", Toast.LENGTH_SHORT).show();
            } else {
                ArrayList<String> items = galleryNames();
                items.add(name);
                prefs.edit().putString("gallery", join(items)).apply();
                currentScreen = 1;
                Toast.makeText(this, "התמונה נוספה לגלריה", Toast.LENGTH_SHORT).show();
            }
            render();
        } catch (Exception e) {
            Toast.makeText(this, "לא ניתן לטעון את התמונה. נסו תמונה אחרת.", Toast.LENGTH_LONG).show();
        }
    }

    private Bitmap loadBitmap(String path, int maxSize) {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, bounds);
        int sample = 1;
        while (bounds.outWidth / sample > maxSize || bounds.outHeight / sample > maxSize) sample *= 2;
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inSampleSize = sample;
        return BitmapFactory.decodeFile(path, opts);
    }

    private long remainingDays() {
        long target = prefs.getLong("weddingDate", 0L);
        if (target == 0L) return -1L;
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
