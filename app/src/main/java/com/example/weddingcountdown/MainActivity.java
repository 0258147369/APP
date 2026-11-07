package com.example.weddingcountdown;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.ViewFlipper;
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
import java.util.Random;

public class MainActivity extends Activity {
    private static final int PICK_IMAGE = 6104;
    private static final String PREFS = "wedding_settings";
    private android.content.SharedPreferences prefs;
    private int currentScreen = 0;
    private int pendingImageAction = 0;
    private int bg, surface, surface2, ink, muted, accent, accentText, heroStart, heroEnd, lineColor;
    private final Handler handler = new Handler();
    private final Runnable midnightRefresh = new Runnable() {
        @Override public void run() {
            if (prefs != null && currentScreen == 0) render();
            scheduleQuoteRefresh();
        }
    };

    private static final String[] QUOTES = new String[]{
        "הדברים היפים ביותר נבנים צעד אחר צעד.",
        "תנו ללב להוביל, ותיהנו גם מהדרך.",
        "כל יום מביא איתו סיבה קטנה לחייך.",
        "הרגעים הקטנים הם אלה שהופכים לזיכרונות גדולים.",
        "לא צריך למהר. הרגע שלכם מתקרב בדיוק בזמן.",
        "בחרו בשמחה, גם בדברים הפשוטים.",
        "הדרך נעימה יותר כשעוצרים להעריך אותה.",
        "יום חדש הוא הזדמנות לעוד זיכרון יפה.",
        "הדברים החשובים באמת נמצאים ברגע הזה.",
        "קחו נשימה עמוקה, וחייכו אל מה שמחכה לכם.",
        "צעד קטן בכל יום מוביל לרגעים גדולים.",
        "יופי אמיתי נמצא בפשטות.",
        "תנו מקום להתרגשות, וגם לשקט.",
        "היום הוא עוד דף בסיפור שאתם יוצרים.",
        "שמרו בלב מקום לכל הדברים הטובים שבדרך.",
        "לפעמים ההמתנה היא חלק מהקסם.",
        "רגעים מיוחדים מתחילים בציפייה קטנה.",
        "הדבר הכי יפה הוא לבנות משהו ביחד.",
        "אל תשכחו ליהנות מהדרך אל היעד.",
        "כל בוקר מקרב אתכם לעוד התחלה מרגשת.",
        "השקט, החיוך והתקווה — אלה הדברים שנשארים.",
        "תנו לכל יום להיות מיוחד בדרכו.",
        "מה שבא מהלב תמיד מרגיש נכון.",
        "היום הזה לא יחזור. עשו ממנו משהו טוב.",
        "התרגשות היא הדרך של הלב לספר שמשהו טוב מתקרב.",
        "גם בתוך ההכנות, זכרו לעצור ולנשום.",
        "הזיכרונות היפים נוצרים כשנוכחים ברגע.",
        "יש יופי בכל התחלה חדשה.",
        "היו עדינים עם עצמכם, ותנו לדברים לקרות.",
        "עוד יום עבר, ועוד רגע יפה מתקרב."
    };

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        applyTheme();
        AlarmReceiver.scheduleNext(this);
        CountdownWidgetProvider.updateAll(this);
        render();
    }

    @Override protected void onResume() {
        super.onResume();
        if (prefs != null) {
            AlarmReceiver.scheduleNext(this);
            CountdownWidgetProvider.updateAll(this);
            scheduleQuoteRefresh();
        }
    }

    @Override protected void onPause() {
        handler.removeCallbacks(midnightRefresh);
        super.onPause();
    }

    private void scheduleQuoteRefresh() {
        handler.removeCallbacks(midnightRefresh);
        Calendar next = Calendar.getInstance();
        next.add(Calendar.DAY_OF_YEAR, 1);
        next.set(Calendar.HOUR_OF_DAY, 0);
        next.set(Calendar.MINUTE, 0);
        next.set(Calendar.SECOND, 2);
        next.set(Calendar.MILLISECOND, 0);
        long delay = Math.max(1000L, next.getTimeInMillis() - System.currentTimeMillis());
        handler.postDelayed(midnightRefresh, delay);
    }

    private void applyTheme() {
        String theme = prefs == null ? "gold" : prefs.getString("theme", "gold");
        if ("soft".equals(theme)) {
            bg = Color.rgb(242, 240, 233);
            surface = Color.WHITE;
            surface2 = Color.rgb(231, 235, 226);
            ink = Color.rgb(47, 59, 51);
            muted = Color.rgb(86, 101, 89);
            accent = Color.rgb(126, 151, 128);
            accentText = Color.WHITE;
            heroStart = Color.rgb(119, 145, 124);
            heroEnd = Color.rgb(54, 79, 66);
            lineColor = Color.argb(85, 255, 255, 255);
        } else if ("midnight".equals(theme)) {
            bg = Color.rgb(11, 18, 40);
            surface = Color.rgb(24, 34, 65);
            surface2 = Color.rgb(35, 47, 82);
            ink = Color.rgb(242, 242, 247);
            muted = Color.rgb(196, 205, 222);
            accent = Color.rgb(232, 199, 126);
            accentText = Color.rgb(22, 29, 48);
            heroStart = Color.rgb(43, 61, 105);
            heroEnd = Color.rgb(16, 25, 55);
            lineColor = Color.argb(95, 235, 222, 186);
        } else {
            bg = Color.rgb(12, 17, 22);
            surface = Color.rgb(25, 34, 41);
            surface2 = Color.rgb(35, 45, 52);
            ink = Color.rgb(239, 237, 232);
            muted = Color.rgb(184, 195, 201);
            accent = Color.rgb(216, 183, 124);
            accentText = Color.rgb(24, 29, 32);
            heroStart = Color.rgb(39, 56, 63);
            heroEnd = Color.rgb(18, 28, 34);
            lineColor = Color.argb(90, 225, 218, 200);
        }
        if (android.os.Build.VERSION.SDK_INT >= 21) {
            getWindow().setStatusBarColor(bg);
            getWindow().setNavigationBarColor(surface);
        }
    }

    private int dp(float value) {
        return (int)(value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private GradientDrawable shape(int color, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radius));
        return d;
    }

    private GradientDrawable outlinedShape(int color, int stroke, int radius) {
        GradientDrawable d = shape(color, radius);
        d.setStroke(dp(1), stroke);
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
        TextView b = label(text, 14, foreground, true);
        b.setPadding(dp(14), dp(12), dp(14), dp(12));
        b.setBackgroundDrawable(shape(background, 15));
        b.setClickable(true);
        b.setFocusable(true);
        b.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { task.run(); }
        });
        return b;
    }

    private LinearLayout card() {
        LinearLayout c = column();
        c.setPadding(dp(19), dp(17), dp(19), dp(17));
        c.setBackgroundDrawable(outlinedShape(surface, Color.argb(42, Color.red(ink), Color.green(ink), Color.blue(ink)), 18));
        if (android.os.Build.VERSION.SDK_INT >= 21) c.setElevation(dp(2));
        return c;
    }

    private void render() {
        applyTheme();
        LinearLayout shell = column();
        shell.setBackgroundColor(bg);
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        shell.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        LinearLayout content = column();
        content.setPadding(dp(20), dp(20), dp(20), dp(22));
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
        nav.setBackgroundColor(surface);
        if (android.os.Build.VERSION.SDK_INT >= 21) nav.setElevation(dp(10));
        String[] names = {"⌂  בית", "▧  גלריה", "⚙  הגדרות"};
        for (int i = 0; i < names.length; i++) {
            final int selected = i;
            TextView item = label(names[i], 13, currentScreen == i ? accent : muted, currentScreen == i);
            item.setClickable(true);
            item.setFocusable(true);
            item.setPadding(dp(7), dp(10), dp(7), dp(10));
            if (currentScreen == i) item.setBackgroundDrawable(shape(surface2, 14));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, -1, 1);
            params.setMargins(dp(4), 0, dp(4), 0);
            nav.addView(item, params);
            item.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { currentScreen = selected; render(); }
            });
        }
        return nav;
    }

    private void sectionTitle(LinearLayout content, String title, String subtitle) {
        content.addView(rightLabel(title, 27, ink, true), lp(-1, 46));
        if (subtitle != null) content.addView(rightLabel(subtitle, 14, muted, false), lp(-1, 27));
        space(content, 14);
    }

    private void buildHome(LinearLayout content) {
        content.addView(rightLabel("הספירה לאחור שלכם", 13, accent, true), lp(-1, 29));
        content.addView(rightLabel("הרגע שלכם מתקרב.", 31, ink, true), lp(-1, 52));
        content.addView(rightLabel("כל יום הוא עוד צעד אל היום הגדול", 14, muted, false), lp(-1, 27));
        space(content, 18);

        final long target = prefs.getLong("weddingDate", 0L);
        final long days = remainingDays();
        final boolean eventDay = target > 0L && days == 0L;
        FrameLayout hero = new FrameLayout(this);
        hero.setBackgroundDrawable(gradient(heroStart, heroEnd, 28));
        LinearLayout.LayoutParams heroParams = lp(-1, eventDay ? 420 : 385);
        heroParams.bottomMargin = dp(16);
        content.addView(hero, heroParams);

        addHeroSlideshow(hero);

        LinearLayout heroContent = column();
        heroContent.setGravity(Gravity.CENTER);
        heroContent.setPadding(dp(20), dp(17), dp(20), dp(17));
        hero.addView(heroContent, new FrameLayout.LayoutParams(-1, -1));

        TextView tag = label(eventDay ? "THE DAY IS HERE" : "UNTIL THE BIG DAY", 10, accent, true);
        if (android.os.Build.VERSION.SDK_INT >= 21) tag.setLetterSpacing(0.16f);
        heroContent.addView(tag, lp(-1, 25));

        String number = target == 0 ? "—" : (eventDay ? "היום!" : (days < 0 ? "♥" : String.valueOf(days)));
        TextView big = label(number, days > 0 ? 92 : 60, Color.WHITE, true);
        big.setShadowLayer(14, 0, 3, Color.argb(100, 0, 0, 0));
        heroContent.addView(big, lp(-1, 132));

        String description = target == 0 ? "התאריך שלכם עוד לא הוגדר" :
                (eventDay ? "זה היום שלכם. תיהנו מכל רגע!" :
                (days < 0 ? "האירוע כבר מאחוריכם" : "ימים עד לרגע המיוחד"));
        heroContent.addView(label(description, 20, Color.WHITE, true), lp(-1, 42));

        View line = new View(this);
        line.setBackgroundColor(lineColor);
        LinearLayout.LayoutParams lineParams = lp(56, 1);
        lineParams.topMargin = dp(9);
        lineParams.bottomMargin = dp(9);
        heroContent.addView(line, lineParams);

        String dateText = target == 0 ? "מגדירים תאריך פעם אחת בהגדרות" :
                new SimpleDateFormat("EEEE  ·  dd.MM.yyyy", new Locale("he")).format(new Date(target));
        heroContent.addView(label(dateText, 15, Color.WHITE, true), lp(-1, 32));

        if (eventDay) {
            ConfettiView confetti = new ConfettiView(this);
            LinearLayout.LayoutParams cp = lp(-1, 78);
            cp.topMargin = dp(4);
            heroContent.addView(confetti, cp);
        } else if (target == 0L) {
            space(heroContent, 8);
            heroContent.addView(label("אפשר להגדיר תאריך במסך ההגדרות", 13, Color.WHITE, false), lp(-1, 31));
        }

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.CENTER);
        actions.addView(action("＋  תמונת רקע", accent, accentText, new Runnable() {
            @Override public void run() { pickImage(0); }
        }), new LinearLayout.LayoutParams(0, dp(52), 1));
        View gap = new View(this);
        actions.addView(gap, lp(10, 1));
        actions.addView(action("הגלריה שלי  ›", surface2, ink, new Runnable() {
            @Override public void run() { currentScreen = 1; render(); }
        }), new LinearLayout.LayoutParams(0, dp(52), 1));
        content.addView(actions, lp(-1, 54));
        space(content, 17);

        if (target > 0L && days > 0L) addProgressCard(content, target);
        else if (eventDay) {
            LinearLayout celebration = card();
            celebration.addView(rightLabel("רגע של חגיגה", 17, accent, true), lp(-1, 28));
            celebration.addView(rightLabel("מאחלים לכם יום מלא בשמחה, חיוכים וזיכרונות יפים.", 14, muted, false), lp(-1, 48));
            content.addView(celebration, lp(-1, -2));
            space(content, 14);
        }

        addDailyQuote(content);
        space(content, 14);
        if (target > 0L && days >= 0L) addMilestonesCard(content, target, days);

        space(content, 14);
        LinearLayout note = card();
        note.setOrientation(LinearLayout.HORIZONTAL);
        note.setGravity(Gravity.CENTER_VERTICAL);
        note.addView(label("✦", 22, accent, true), lp(34, 42));
        LinearLayout noteText = column();
        noteText.addView(rightLabel("הרגעים שלכם, במקום אחד", 14, ink, true), lp(-1, 25));
        noteText.addView(rightLabel("התמונות וההגדרות נשמרות במכשיר בלבד", 12, muted, false), lp(-1, 22));
        note.addView(noteText, new LinearLayout.LayoutParams(0, -2, 1));
        content.addView(note, lp(-1, 76));
    }

    private void addHeroSlideshow(FrameLayout hero) {
        ArrayList<String> images = new ArrayList<String>();
        String cover = prefs.getString("cover", "");
        if (cover.length() > 0 && new File(getFilesDir(), cover).exists()) images.add(cover);
        for (String name : galleryNames()) {
            if (!images.contains(name) && new File(getFilesDir(), name).exists()) images.add(name);
        }
        if (images.isEmpty()) return;
        ViewFlipper flipper = new ViewFlipper(this);
        flipper.setFlipInterval(6500);
        flipper.setInAnimation(AnimationUtils.loadAnimation(this, android.R.anim.fade_in));
        flipper.setOutAnimation(AnimationUtils.loadAnimation(this, android.R.anim.fade_out));
        for (String name : images) {
            ImageView image = new ImageView(this);
            image.setScaleType(ImageView.ScaleType.CENTER_CROP);
            image.setImageBitmap(loadBitmap(new File(getFilesDir(), name).getAbsolutePath(), 1200));
            flipper.addView(image, new FrameLayout.LayoutParams(-1, -1));
        }
        hero.addView(flipper, new FrameLayout.LayoutParams(-1, -1));
        if (images.size() > 1) flipper.startFlipping();
        View shade = new View(this);
        shade.setBackgroundDrawable(gradient(Color.argb(185, 5, 10, 15), Color.argb(230, 5, 10, 15), 28));
        hero.addView(shade, new FrameLayout.LayoutParams(-1, -1));
    }

    private void addProgressCard(LinearLayout content, long target) {
        LinearLayout progress = card();
        progress.addView(rightLabel("הדרך שעברתם", 17, ink, true), lp(-1, 27));
        space(progress, 8);
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        ProgressRing ring = new ProgressRing(this, progressPercent(target));
        row.addView(ring, lp(138, 138));
        LinearLayout details = column();
        details.setPadding(dp(14), 0, 0, 0);
        details.addView(rightLabel("כל יום מקרב אתכם", 17, ink, true), lp(-1, 32));
        details.addView(rightLabel("עוד קצת סבלנות, ואתם שם.", 14, muted, false), lp(-1, 29));
        int pct = Math.round(progressPercent(target) * 100f);
        details.addView(rightLabel(pct + "% מהדרך", 20, accent, true), lp(-1, 35));
        row.addView(details, new LinearLayout.LayoutParams(0, -2, 1));
        progress.addView(row, lp(-1, -2));
        content.addView(progress, lp(-1, -2));
        space(content, 14);
    }

    private float progressPercent(long target) {
        long start = prefs.getLong("weddingStart", 0L);
        if (start <= 0L) {
            Calendar c = Calendar.getInstance();
            c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0);
            c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0);
            start = c.getTimeInMillis();
        }
        long now = System.currentTimeMillis();
        float total = (float)(target - start);
        if (total <= 0) return 1f;
        return Math.max(0f, Math.min(1f, (float)(now - start) / total));
    }

    private void addDailyQuote(LinearLayout content) {
        LinearLayout quote = card();
        quote.setBackgroundDrawable(gradient(surface2, surface, 20));
        TextView small = rightLabel("✦  משפט להיום", 13, accent, true);
        quote.addView(small, lp(-1, 26));
        String q = dailyQuote();
        TextView text = rightLabel("“" + q + "”", 18, ink, true);
        text.setLineSpacing(dp(4), 1.0f);
        quote.addView(text, lp(-1, -2));
        TextView foot = rightLabel("משפט חדש בכל יום  ·  פועל גם ללא אינטרנט", 12, muted, false);
        LinearLayout.LayoutParams fp = lp(-1, 23);
        fp.topMargin = dp(8);
        quote.addView(foot, fp);
        content.addView(quote, lp(-1, -2));
    }

    private String dailyQuote() {
        Calendar c = Calendar.getInstance();
        int index = Math.abs(c.get(Calendar.YEAR) * 31 + c.get(Calendar.DAY_OF_YEAR)) % QUOTES.length;
        return QUOTES[index];
    }

    private void addMilestonesCard(LinearLayout content, long target, long remaining) {
        LinearLayout milestones = card();
        milestones.addView(rightLabel("ציוני הדרך שלכם", 17, ink, true), lp(-1, 29));
        int[] marks = new int[]{100, 50, 30, 14, 7, 3, 1, 0};
        for (int mark : marks) {
            LinearLayout row = new LinearLayout(this);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, dp(5), 0, dp(5));
            boolean reached = remaining <= mark;
            TextView icon = label(reached ? "✓" : "○", 16, reached ? accent : muted, true);
            row.addView(icon, lp(30, 32));
            LinearLayout textColumn = column();
            textColumn.addView(rightLabel(mark == 0 ? "היום הגדול" : mark + " ימים לפני", 15, ink, true), lp(-1, 23));
            long daysToMark = remaining - mark;
            String state = reached ? "הושג" : "בעוד " + daysToMark + " ימים";
            textColumn.addView(rightLabel(state, 12, reached ? accent : muted, false), lp(-1, 20));
            row.addView(textColumn, new LinearLayout.LayoutParams(0, -2, 1));
            TextView date = label(new SimpleDateFormat("dd.MM", Locale.getDefault()).format(milestoneDate(target, mark)), 12, muted, true);
            row.addView(date, lp(62, 32));
            milestones.addView(row, lp(-1, 48));
            if (mark != 0) {
                View divider = new View(this);
                divider.setBackgroundColor(Color.argb(25, 150, 160, 160));
                milestones.addView(divider, lp(-1, 1));
            }
        }
        content.addView(milestones, lp(-1, -2));
    }

    private Date milestoneDate(long target, int daysBefore) {
        Calendar date = Calendar.getInstance();
        date.setTimeInMillis(target);
        date.add(Calendar.DAY_OF_YEAR, -daysBefore);
        return date.getTime();
    }

    private void buildGallery(LinearLayout content) {
        sectionTitle(content, "הגלריה שלכם", "התמונות האהובות, בתצוגה שאפשר להחליק");
        content.addView(action("＋  הוספת תמונה לגלריה", accent, accentText, new Runnable() {
            @Override public void run() { pickImage(1); }
        }), lp(-1, 50));
        space(content, 15);

        String cover = prefs.getString("cover", "");
        if (cover.length() > 0) {
            File f = new File(getFilesDir(), cover);
            if (f.exists()) {
                content.addView(rightLabel("תמונת הנושא", 16, ink, true), lp(-1, 29));
                addLargePhoto(content, f, false);
                space(content, 15);
            }
        }

        ArrayList<String> names = galleryNames();
        if (names.isEmpty()) {
            LinearLayout empty = card();
            empty.addView(label("התמונות שתוסיפו יופיעו כאן", 15, muted, false), lp(-1, 80));
            content.addView(empty, lp(-1, 110));
        } else {
            content.addView(rightLabel("החליקו כדי לראות את כל התמונות", 14, muted, false), lp(-1, 28));
            HorizontalScrollView horizontal = new HorizontalScrollView(this);
            horizontal.setHorizontalScrollBarEnabled(false);
            LinearLayout strip = new LinearLayout(this);
            strip.setOrientation(LinearLayout.HORIZONTAL);
            for (String name : names) {
                File f = new File(getFilesDir(), name);
                if (!f.exists()) continue;
                LinearLayout tile = column();
                tile.setPadding(dp(8), dp(8), dp(8), dp(8));
                tile.setBackgroundDrawable(shape(surface, 18));
                ImageView image = new ImageView(this);
                image.setScaleType(ImageView.ScaleType.CENTER_CROP);
                image.setImageBitmap(loadBitmap(f.getAbsolutePath(), 700));
                tile.addView(image, lp(220, 175));
                TextView caption = label("תמונה  ·  הקשה לאפשרויות", 11, muted, false);
                tile.addView(caption, lp(220, 34));
                tile.setClickable(true);
                tile.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View v) { showPhotoOptions(f, true); }
                });
                LinearLayout.LayoutParams tp = lp(236, -2);
                tp.setMargins(0, 0, dp(10), 0);
                strip.addView(tile, tp);
            }
            horizontal.addView(strip);
            content.addView(horizontal, lp(-1, 230));
        }
    }

    private void addLargePhoto(LinearLayout content, final File file, final boolean fromGallery) {
        ImageView image = new ImageView(this);
        image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        image.setImageBitmap(loadBitmap(file.getAbsolutePath(), 1100));
        image.setBackgroundDrawable(shape(surface2, 18));
        content.addView(image, lp(-1, 210));
        image.setClickable(true);
        image.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { showPhotoOptions(file, fromGallery); }
        });
    }

    private void showPhotoOptions(final File file, final boolean fromGallery) {
        final String[] options = fromGallery
                ? new String[]{"הצגה בגודל מלא", "הגדרה כתמונת נושא", "הסרה מהגלריה", "ביטול"}
                : new String[]{"הצגה בגודל מלא", "החלפת תמונת הנושא", "ביטול"};
        new AlertDialog.Builder(this).setTitle("אפשרויות לתמונה").setItems(options, (dialog, which) -> {
            if (fromGallery && which == 0) showPhotoDialog(file);
            else if (fromGallery && which == 1) {
                prefs.edit().putString("cover", file.getName()).apply();
                currentScreen = 0; render();
            } else if (fromGallery && which == 2) {
                removeFromGallery(file.getName()); render();
            } else if (!fromGallery && which == 0) showPhotoDialog(file);
            else if (!fromGallery && which == 1) pickImage(0);
        }).show();
    }

    private void showPhotoDialog(File file) {
        Dialog dialog = new Dialog(this);
        ImageView image = new ImageView(this);
        image.setImageBitmap(loadBitmap(file.getAbsolutePath(), 1400));
        image.setScaleType(ImageView.ScaleType.FIT_CENTER);
        dialog.setContentView(image);
        android.view.Window w = dialog.getWindow();
        if (w != null) {
            w.setBackgroundDrawableResource(android.R.color.black);
            w.setLayout(-1, -1);
        }
        dialog.show();
        if (w != null) w.setLayout(-1, -1);
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
        sectionTitle(content, "הגדרות", "התאימו את הספירה בדיוק לסגנון שלכם");

        LinearLayout dateCard = card();
        dateCard.addView(rightLabel("תאריך החתונה", 17, ink, true), lp(-1, 29));
        long date = prefs.getLong("weddingDate", 0L);
        String dateText = date == 0 ? "לא נבחר תאריך" : new SimpleDateFormat("EEEE, dd.MM.yyyy", new Locale("he")).format(new Date(date));
        dateCard.addView(rightLabel(dateText, 14, muted, false), lp(-1, 27));
        space(dateCard, 7);
        dateCard.addView(action("בחירת תאריך", accent, accentText, new Runnable() {
            @Override public void run() { chooseDate(); }
        }), lp(-1, 47));
        content.addView(dateCard, lp(-1, -2));
        space(content, 14);

        buildThemePicker(content);
        space(content, 14);
        buildNotificationSettings(content);
        space(content, 14);

        LinearLayout quoteCard = card();
        quoteCard.addView(rightLabel("המשפט היומי", 17, ink, true), lp(-1, 29));
        quoteCard.addView(rightLabel("בכל יום מופיע משפט חדש מתוך אוסף מובנה באפליקציה. המשפט נשאר קבוע לאורך כל היום ועובד גם ללא אינטרנט.", 13, muted, false), lp(-1, 58));
        content.addView(quoteCard, lp(-1, -2));
        space(content, 14);

        LinearLayout widget = card();
        widget.addView(rightLabel("ווידג׳ט למסך הבית", 17, ink, true), lp(-1, 30));
        widget.addView(rightLabel("הוסיפו את ווידג׳ט הספירה דרך תפריט הווידג׳טים של הטלפון. צבעו יתעדכן לפי העיצוב שבחרתם.", 13, muted, false), lp(-1, 50));
        content.addView(widget, lp(-1, -2));
        space(content, 14);

        LinearLayout offline = card();
        offline.addView(rightLabel("שמירה ופרטיות", 17, ink, true), lp(-1, 29));
        offline.addView(rightLabel("התאריך, התמונות והבחירות נשמרים במכשיר בלבד. אין צורך בחשבון או בחיבור לאינטרנט.", 13, muted, false), lp(-1, 48));
        content.addView(offline, lp(-1, -2));
    }

    private void buildThemePicker(LinearLayout content) {
        LinearLayout themeCard = card();
        themeCard.addView(rightLabel("בחרו את העיצוב שלכם", 17, ink, true), lp(-1, 30));
        themeCard.addView(rightLabel("אפשר להחליף סגנון בכל רגע", 12, muted, false), lp(-1, 22));
        space(themeCard, 9);
        addThemeOption(themeCard, "gold", "LUXURY GOLD", "שחור וזהב", Color.rgb(12,17,22), Color.rgb(216,183,124));
        space(themeCard, 8);
        addThemeOption(themeCard, "soft", "SOFT MINIMAL", "שמנת וירוק מרווה", Color.rgb(242,240,233), Color.rgb(126,151,128));
        space(themeCard, 8);
        addThemeOption(themeCard, "midnight", "MIDNIGHT", "כחול לילה וזהב", Color.rgb(11,18,40), Color.rgb(232,199,126));
        content.addView(themeCard, lp(-1, -2));
    }

    private void addThemeOption(LinearLayout parent, final String key, String title, String subtitle, int sampleBg, int sampleAccent) {
        final boolean selected = key.equals(prefs.getString("theme", "gold"));
        LinearLayout option = new LinearLayout(this);
        option.setGravity(Gravity.CENTER_VERTICAL);
        option.setPadding(dp(10), dp(9), dp(10), dp(9));
        option.setBackgroundDrawable(outlinedShape(selected ? surface2 : surface, selected ? accent : Color.argb(35, 150, 160, 160), 15));
        View swatch = new View(this);
        swatch.setBackgroundDrawable(gradient(sampleBg, sampleAccent, 10));
        option.addView(swatch, lp(48, 48));
        LinearLayout texts = column();
        texts.setPadding(dp(11), 0, dp(8), 0);
        texts.addView(rightLabel(title, 12, ink, true), lp(-1, 22));
        texts.addView(rightLabel(subtitle, 12, muted, false), lp(-1, 21));
        option.addView(texts, new LinearLayout.LayoutParams(0, -2, 1));
        option.addView(label(selected ? "✓" : "○", 20, selected ? accent : muted, true), lp(28, 35));
        option.setClickable(true);
        option.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                prefs.edit().putString("theme", key).apply();
                applyTheme();
                render();
                CountdownWidgetProvider.updateAll(MainActivity.this);
            }
        });
        parent.addView(option, lp(-1, 65));
    }

    private void buildNotificationSettings(LinearLayout content) {
        LinearLayout alerts = card();
        alerts.addView(rightLabel("התראות", 18, ink, true), lp(-1, 32));
        addSwitch(alerts, "התראה יומית", "daily", true);
        addSwitch(alerts, "התראות על ציוני דרך", "milestones", true);
        addSwitch(alerts, "השתקת כל ההתראות", "muted", false);
        TextView time = action("שעת התראה: " + prefs.getString("alertTime", "09:00"), surface2, accent, new Runnable() {
            @Override public void run() { chooseTime(); }
        });
        LinearLayout.LayoutParams timeParams = lp(-1, 46);
        timeParams.topMargin = dp(8);
        alerts.addView(time, timeParams);
        content.addView(alerts, lp(-1, -2));
    }

    private void addSwitch(LinearLayout parent, String title, final String key, boolean defaultValue) {
        Switch s = new Switch(this);
        s.setText(title);
        s.setTextSize(14);
        s.setTextColor(ink);
        s.setChecked(prefs.getBoolean(key, defaultValue));
        s.setOnCheckedChangeListener((button, checked) -> {
            prefs.edit().putBoolean(key, checked).apply();
            AlarmReceiver.scheduleNext(MainActivity.this);
        });
        parent.addView(s, lp(-1, 47));
    }

    private void chooseDate() {
        Calendar c = Calendar.getInstance();
        long stored = prefs.getLong("weddingDate", 0L);
        if (stored > 0) c.setTimeInMillis(stored);
        DatePickerDialog dlg = new DatePickerDialog(this, (view, year, month, day) -> {
            Calendar selected = Calendar.getInstance();
            selected.set(year, month, day, 0, 0, 0);
            selected.set(Calendar.MILLISECOND, 0);
            android.content.SharedPreferences.Editor editor = prefs.edit().putLong("weddingDate", selected.getTimeInMillis());
            if (prefs.getLong("weddingStart", 0L) == 0L) {
                Calendar start = Calendar.getInstance();
                start.set(Calendar.HOUR_OF_DAY, 0); start.set(Calendar.MINUTE, 0);
                start.set(Calendar.SECOND, 0); start.set(Calendar.MILLISECOND, 0);
                editor.putLong("weddingStart", start.getTimeInMillis());
            }
            editor.apply();
            AlarmReceiver.scheduleNext(MainActivity.this);
            CountdownWidgetProvider.updateAll(MainActivity.this);
            currentScreen = 0;
            render();
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
        dlg.show();
    }

    private void chooseTime() {
        String[] parts = prefs.getString("alertTime", "09:00").split(":");
        int hour = 9, minute = 0;
        try { hour = Integer.parseInt(parts[0]); minute = Integer.parseInt(parts[1]); } catch (Exception ignored) { }
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
                Toast.makeText(this, "תמונת הנושא עודכנה", Toast.LENGTH_SHORT).show();
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

    private class ProgressRing extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF oval = new RectF();
        private final float progress;
        ProgressRing(Activity context, float value) { super(context); progress = value; }
        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float cx = getWidth() / 2f, cy = getHeight() / 2f;
            float radius = Math.min(cx, cy) - dp(11);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(11));
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setColor(Color.argb(45, Color.red(muted), Color.green(muted), Color.blue(muted)));
            oval.set(cx - radius, cy - radius, cx + radius, cy + radius);
            canvas.drawArc(oval, -90, 360, false, paint);
            paint.setColor(accent);
            canvas.drawArc(oval, -90, 360 * progress, false, paint);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(ink);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
            paint.setTextSize(dp(22));
            canvas.drawText(Math.round(progress * 100) + "%", cx, cy + dp(7), paint);
        }
    }

    private class ConfettiView extends View implements Runnable {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Handler animationHandler = new Handler();
        private final Random random = new Random(42);
        private final float[] x = new float[32];
        private final float[] y = new float[32];
        private final float[] speed = new float[32];
        private final int[] colors = new int[]{Color.rgb(232,199,126), Color.rgb(255,255,255), Color.rgb(126,151,128), Color.rgb(210,151,165)};
        private boolean active = true;
        ConfettiView(Activity context) {
            super(context);
            for (int i=0;i<x.length;i++) {
                x[i] = random.nextFloat();
                y[i] = random.nextFloat();
                speed[i] = 0.004f + random.nextFloat() * 0.012f;
            }
        }
        @Override protected void onAttachedToWindow() {
            super.onAttachedToWindow();
            active = true;
            animationHandler.post(this);
        }
        @Override protected void onDetachedFromWindow() {
            active = false;
            animationHandler.removeCallbacks(this);
            super.onDetachedFromWindow();
        }
        @Override public void run() {
            if (!active) return;
            for (int i=0;i<y.length;i++) {
                y[i] += speed[i];
                if (y[i] > 1.1f) { y[i] = -0.1f; x[i] = random.nextFloat(); }
            }
            invalidate();
            animationHandler.postDelayed(this, 45);
        }
        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            for (int i=0;i<x.length;i++) {
                paint.setColor(colors[i % colors.length]);
                canvas.save();
                canvas.rotate((i * 31 + y[i] * 360) % 360, x[i] * getWidth(), y[i] * getHeight());
                canvas.drawRect(x[i] * getWidth(), y[i] * getHeight(), x[i] * getWidth() + dp(5), y[i] * getHeight() + dp(10), paint);
                canvas.restore();
            }
        }
    }
}
