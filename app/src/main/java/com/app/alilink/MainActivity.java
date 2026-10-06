package com.app.alilink;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends android.app.Activity {

    private static final String PREFS = "alilink_prefs";
    private static final String KEY_ENDPOINT = "backend_endpoint";
    private static final String KEY_HISTORY = "history";

    private LinearLayout root;
    private EditText urlInput;
    private EditText endpointInput;
    private TextView resultText;
    private TextView statusText;
    private LinearLayout historyList;

    private final int ink = 0xff152033;
    private final int muted = 0xff67738a;
    private final int background = 0xfff4f7fb;
    private final int surface = 0xffffffff;
    private final int primary = 0xff2f6bff;
    private final int success = 0xff168b57;
    private final int danger = 0xffc93535;
    private final int line = 0xffe2e8f0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(background);
        getWindow().setNavigationBarColor(background);
        showHome();
        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        showHome();
        handleIntent(intent);
    }

    private void showHome() {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(background);
        setContentView(root);

        LinearLayout toolbar = new LinearLayout(this);
        toolbar.setOrientation(LinearLayout.HORIZONTAL);
        toolbar.setGravity(Gravity.CENTER_VERTICAL);
        toolbar.setPadding(dp(20), dp(12), dp(12), dp(6));

        LinearLayout titleBox = new LinearLayout(this);
        titleBox.setOrientation(LinearLayout.VERTICAL);
        toolbar.addView(titleBox, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView title = label("AliLink", 28, ink, true);
        titleBox.addView(title);

        TextView subtitle = label("הופכים קישור AliExpress לקישור שיווקי", 14, muted, false);
        titleBox.addView(subtitle);

        TextView settings = label("⚙", 26, ink, true);
        settings.setGravity(Gravity.CENTER);
        settings.setPadding(dp(12), dp(6), dp(12), dp(6));
        settings.setOnClickListener(v -> showSettings());
        toolbar.addView(settings);

        root.addView(toolbar);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(18), dp(8), dp(18), dp(22));
        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        LinearLayout hero = card();
        hero.setPadding(dp(18), dp(18), dp(18), dp(18));

        TextView badge = label("●  מוכן להמרה", 13, success, true);
        hero.addView(badge);

        TextView h = label("הדבק קישור אחד.\nהשאר נעשה אוטומטית.", 25, ink, true);
        h.setPadding(0, dp(10), 0, dp(6));
        hero.addView(h);

        TextView help = label("אפשר גם ללחוץ על קישור AliExpress שקיבלת או לשתף אותו ישירות לאפליקציה.", 14, muted, false);
        hero.addView(help);

        content.addView(hero, matchWrap(0));

        LinearLayout inputCard = card();
        inputCard.setPadding(dp(16), dp(16), dp(16), dp(16));

        TextView inTitle = label("קישור AliExpress", 15, ink, true);
        inputCard.addView(inTitle);

        urlInput = field("https://www.aliexpress.com/item/...", false);
        inputCard.addView(urlInput, matchWrap(10));

        LinearLayout pasteRow = new LinearLayout(this);
        pasteRow.setOrientation(LinearLayout.HORIZONTAL);
        pasteRow.setGravity(Gravity.CENTER_VERTICAL);

        Button paste = button("הדבק מהלוח", false);
        paste.setOnClickListener(v -> pasteClipboard());
        pasteRow.addView(paste, new LinearLayout.LayoutParams(0, dp(48), 1f));

        spaceGap(pasteRow, 8);

        Button clear = button("נקה", false);
        clear.setOnClickListener(v -> {
            urlInput.setText("");
            clearResult();
        });
        pasteRow.addView(clear, new LinearLayout.LayoutParams(0, dp(48), 1f));

        inputCard.addView(pasteRow, matchWrap(10));
        content.addView(inputCard, matchWrap(12));

        statusText = label("", 14, muted, false);
        statusText.setPadding(dp(4), 0, dp(4), dp(4));
        content.addView(statusText, matchWrap(2));

        Button convert = button("המר לקישור שיווקי", true);
        convert.setOnClickListener(v -> convertFromInput());
        content.addView(convert, matchWrap(4));

        LinearLayout resultCard = card();
        resultCard.setPadding(dp(16), dp(16), dp(16), dp(16));

        TextView resultTitle = label("הקישור שנוצר", 15, ink, true);
        resultCard.addView(resultTitle);

        resultText = label("עדיין אין קישור. לחץ על "המר".", 14, muted, false);
        resultText.setTextIsSelectable(true);
        resultText.setPadding(0, dp(10), 0, dp(10));
        resultCard.addView(resultText);

        LinearLayout resultButtons = new LinearLayout(this);
        resultButtons.setOrientation(LinearLayout.HORIZONTAL);

        Button copy = button("העתק", true);
        copy.setOnClickListener(v -> copyResult());
        resultButtons.addView(copy, new LinearLayout.LayoutParams(0, dp(46), 1f));

        spaceGap(resultButtons, 8);

        Button share = button("שתף", false);
        share.setOnClickListener(v -> shareResult());
        resultButtons.addView(share, new LinearLayout.LayoutParams(0, dp(46), 1f));

        spaceGap(resultButtons, 8);

        Button open = button("פתח", false);
        open.setOnClickListener(v -> openResult());
        resultButtons.addView(open, new LinearLayout.LayoutParams(0, dp(46), 1f));

        resultCard.addView(resultButtons, matchWrap(2));
        content.addView(resultCard, matchWrap(12));

        LinearLayout historyCard = card();
        historyCard.setPadding(dp(16), dp(16), dp(16), dp(16));

        TextView histTitle = label("המרות אחרונות", 15, ink, true);
        historyCard.addView(histTitle);

        historyList = new LinearLayout(this);
        historyList.setOrientation(LinearLayout.VERTICAL);
        historyCard.addView(historyList, matchWrap(10));
        content.addView(historyCard, matchWrap(0));

        refreshHistory();
    }

    private void showSettings() {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(background);
        setContentView(root);

        LinearLayout toolbar = new LinearLayout(this);
        toolbar.setOrientation(LinearLayout.HORIZONTAL);
        toolbar.setGravity(Gravity.CENTER_VERTICAL);
        toolbar.setPadding(dp(14), dp(12), dp(14), dp(6));

        TextView back = label("‹", 36, ink, true);
        back.setPadding(dp(8), 0, dp(18), 0);
        back.setOnClickListener(v -> showHome());
        toolbar.addView(back);

        TextView title = label("הגדרות", 25, ink, true);
        toolbar.addView(title);
        root.addView(toolbar);

        ScrollView scroll = new ScrollView(this);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(18), dp(12), dp(18), dp(22));
        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        LinearLayout card = card();
        card.setPadding(dp(18), dp(18), dp(18), dp(18));

        TextView heading = label("חיבור לממיר Affiliate", 22, ink, true);
        card.addView(heading);

        TextView copy = label("כדי שכל קישור מוצר יקבל קישור Affiliate חדש שמתאים בדיוק ליעד שלו, האפליקציה שולחת את כתובת המוצר לשרת שלך. מפתחות AliExpress נשארים בשרת ולא בתוך ה־APK.", 14, muted, false);
        copy.setPadding(0, dp(8), 0, dp(14));
        card.addView(copy);

        endpointInput = field("https://your-server.example/convert", false);
        endpointInput.setText(getPrefs().getString(KEY_ENDPOINT, ""));
        card.addView(endpointInput);

        Button save = button("שמור כתובת שרת", true);
        save.setOnClickListener(v -> {
            String endpoint = endpointInput.getText().toString().trim();
            if (!endpoint.startsWith("https://")) {
                Toast.makeText(this, "יש להזין כתובת שרת HTTPS", Toast.LENGTH_SHORT).show();
                return;
            }
            getPrefs().edit().putString(KEY_ENDPOINT, endpoint).apply();
            Toast.makeText(this, "נשמר בהצלחה", Toast.LENGTH_SHORT).show();
            showHome();
        });
        card.addView(save, matchWrap(14));

        content.addView(card, matchWrap(12));

        LinearLayout info = card();
        info.setPadding(dp(18), dp(18), dp(18), dp(18));
        TextView infoTitle = label("מה השרת עושה?", 18, ink, true);
        info.addView(infoTitle);

        TextView infoText = label("השרת מקבל URL של מוצר, קורא ל־AliExpress Affiliate Link Generator עם ה־Tracking ID שלך, ומחזיר לאפליקציה את קישור ה־Affiliate שנוצר עבור אותו URL.", 14, muted, false);
        infoText.setPadding(0, dp(8), 0, 0);
        info.addView(infoText);
        content.addView(info, matchWrap(0));

        Button clearHistory = button("מחק היסטוריה", false);
        clearHistory.setOnClickListener(v -> {
            getPrefs().edit().remove(KEY_HISTORY).apply();
            Toast.makeText(this, "ההיסטוריה נמחקה", Toast.LENGTH_SHORT).show();
        });
        content.addView(clearHistory, matchWrap(12));
    }

    private void handleIntent(Intent intent) {
        if (intent == null || urlInput == null) return;

        String data = null;
        if (Intent.ACTION_VIEW.equals(intent.getAction()) && intent.getData() != null) {
            data = intent.getDataString();
        } else if (Intent.ACTION_SEND.equals(intent.getAction())) {
            data = intent.getStringExtra(Intent.EXTRA_TEXT);
        }

        if (!TextUtils.isEmpty(data)) {
            String link = extractUrl(data);
            if (link != null) {
                urlInput.setText(link);
                urlInput.setSelection(urlInput.length());
                convertFromInput();
            }
        }
    }

    private void convertFromInput() {
        String raw = urlInput.getText().toString().trim();
        String url = extractUrl(raw);
        if (url == null) {
            setStatus("לא מצאתי בקישור כתובת AliExpress תקינה.", danger);
            return;
        }

        Uri parsed = Uri.parse(url);
        if (!isAliExpressHost(parsed.getHost())) {
            setStatus("הכתובת אינה שייכת ל־AliExpress.", danger);
            return;
        }

        if (isAffiliateHost(parsed.getHost()) && url.contains("/e/")) {
            resultText.setText(url);
            setStatus("זה כבר נראה כמו קישור Affiliate של AliExpress — לא המרתי אותו שוב.", success);
            saveHistory(url);
            return;
        }

        String endpoint = getPrefs().getString(KEY_ENDPOINT, "").trim();
        if (endpoint.isEmpty()) {
            setStatus("צריך להגדיר פעם אחת את כתובת שרת ההמרה דרך ⚙.", danger);
            return;
        }

        convertViaServer(endpoint, url);
    }

    private void convertViaServer(String endpoint, String sourceUrl) {
        setStatus("ממיר את הקישור…", muted);
        new Thread(() -> {
            java.net.HttpURLConnection connection = null;
            try {
                java.net.URL api = new java.net.URL(endpoint);
                connection = (java.net.HttpURLConnection) api.openConnection();
                connection.setRequestMethod("POST");
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(20000);
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                String escaped = sourceUrl.replace("\\", "\\\\").replace("\"", "\\\"");
                String body = "{\"url\":\"" + escaped + "\"}";
                byte[] bytes = body.getBytes(java.nio.charset.StandardCharsets.UTF_8);
                connection.getOutputStream().write(bytes);

                int code = connection.getResponseCode();
                java.io.InputStream stream = code >= 200 && code < 300
                        ? connection.getInputStream() : connection.getErrorStream();
                java.io.BufferedReader reader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(stream, java.nio.charset.StandardCharsets.UTF_8));
                StringBuilder response = new StringBuilder();
                String lineText;
                while ((lineText = reader.readLine()) != null) response.append(lineText);

                String json = response.toString();
                String affiliate = extractJsonString(json, "affiliateUrl");
                String error = extractJsonString(json, "error");

                runOnUiThread(() -> {
                    if (code >= 200 && code < 300 && affiliate != null && affiliate.startsWith("http")) {
                        resultText.setText(affiliate);
                        setStatus("נוצר קישור Affiliate עבור המוצר הספציפי.", success);
                        saveHistory(affiliate);
                    } else {
                        setStatus(error != null ? error : "השרת לא החזיר קישור תקין.", danger);
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> setStatus("שגיאה בחיבור לשרת ההמרה.", danger));
            } finally {
                if (connection != null) connection.disconnect();
            }
        }).start();
    }

    private String extractJsonString(String json, String key) {
        if (json == null) return null;
        String needle = "\"" + key + "\":\"";
        int start = json.indexOf(needle);
        if (start < 0) return null;
        start += needle.length();
        StringBuilder out = new StringBuilder();
        boolean escaped = false;
        for (int i = start; i < json.length(); i++) {
            char ch = json.charAt(i);
            if (escaped) {
                if (ch == 'n') out.append('\\n');
                else out.append(ch);
                escaped = false;
            } else if (ch == '\\') {
                escaped = true;
            } else if (ch == '"') {
                return out.toString();
            } else {
                out.append(ch);
            }
        }
        return null;
    }

    private void pasteClipboard() {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null && clipboard.hasPrimaryClip()) {
            ClipData clip = clipboard.getPrimaryClip();
            if (clip != null && clip.getItemCount() > 0) {
                CharSequence text = clip.getItemAt(0).coerceToText(this);
                if (text != null) {
                    urlInput.setText(text.toString());
                    urlInput.setSelection(urlInput.length());
                    setStatus("הודבק מהלוח.", muted);
                }
            }
        }
    }

    private void copyResult() {
        String value = resultText == null ? "" : resultText.getText().toString().trim();
        if (!value.startsWith("http")) {
            Toast.makeText(this, "אין קישור להעתקה", Toast.LENGTH_SHORT).show();
            return;
        }
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(ClipData.newPlainText("AliLink", value));
        Toast.makeText(this, "הקישור הועתק", Toast.LENGTH_SHORT).show();
    }

    private void shareResult() {
        String value = resultText == null ? "" : resultText.getText().toString().trim();
        if (!value.startsWith("http")) {
            Toast.makeText(this, "אין קישור לשיתוף", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("text/plain");
        share.putExtra(Intent.EXTRA_TEXT, value);
        startActivity(Intent.createChooser(share, "שתף קישור"));
    }

    private void openResult() {
        String value = resultText == null ? "" : resultText.getText().toString().trim();
        if (!value.startsWith("http")) {
            Toast.makeText(this, "אין קישור לפתיחה", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(value)));
        } catch (Exception e) {
            Toast.makeText(this, "לא נמצא דפדפן לפתיחת הקישור", Toast.LENGTH_SHORT).show();
        }
    }

    private void clearResult() {
        if (resultText != null) resultText.setText("עדיין אין קישור. לחץ על המרה.");
        if (statusText != null) statusText.setText("");
    }

    private String extractUrl(String text) {
        if (TextUtils.isEmpty(text)) return null;

        Matcher m = Pattern.compile("https?://[^\\s]+", Pattern.CASE_INSENSITIVE).matcher(text);
        if (!m.find()) return null;

        String url = m.group().trim();
        while (url.endsWith(")")
                || url.endsWith("]")
                || url.endsWith("}")
                || url.endsWith(",")
                || url.endsWith(".")
                || url.endsWith(";")) {
            url = url.substring(0, url.length() - 1);
        }
        return url;
    }

    private boolean isAliExpressHost(String host) {
        if (TextUtils.isEmpty(host)) return false;
        host = host.toLowerCase();
        return host.equals("aliexpress.com") || host.endsWith(".aliexpress.com");
    }

    private boolean isAffiliateHost(String host) {
        return host != null && host.equalsIgnoreCase("s.click.aliexpress.com");
    }

    private void saveHistory(String url) {
        ArrayList<String> values = new ArrayList<>();
        String current = getPrefs().getString(KEY_HISTORY, "");

        if (!TextUtils.isEmpty(current)) {
            for (String item : current.split("\\n")) {
                if (!item.trim().isEmpty()) values.add(item);
            }
        }

        values.remove(url);
        values.add(0, url);
        while (values.size() > 8) values.remove(values.size() - 1);

        StringBuilder out = new StringBuilder();
        for (String item : values) {
            if (out.length() > 0) out.append("\\n");
            out.append(item);
        }

        getPrefs().edit().putString(KEY_HISTORY, out.toString()).apply();

        if (historyList != null) refreshHistory();
    }

    private void refreshHistory() {
        if (historyList == null) return;
        historyList.removeAllViews();

        String current = getPrefs().getString(KEY_HISTORY, "");
        if (TextUtils.isEmpty(current)) {
            TextView empty = label("כאן יופיעו ההמרות האחרונות שלך.", 13, muted, false);
            historyList.addView(empty);
            return;
        }

        for (String item : current.split("\\n")) {
            if (item.trim().isEmpty()) continue;

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, dp(5), 0, dp(5));

            TextView txt = label(shorten(item, 52), 13, ink, false);
            txt.setTextIsSelectable(true);
            txt.setOnClickListener(v -> {
                resultText.setText(item);
                setStatus("נטען מההיסטוריה.", muted);
            });
            row.addView(txt, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            Button c = button("העתק", false);
            c.setOnClickListener(v -> {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                clipboard.setPrimaryClip(ClipData.newPlainText("AliLink", item));
                Toast.makeText(this, "הועתק", Toast.LENGTH_SHORT).show();
            });
            row.addView(c, new LinearLayout.LayoutParams(dp(78), dp(40)));

            historyList.addView(row);
        }
    }

    private String shorten(String text, int max) {
        if (text.length() <= max) return text;
        return text.substring(0, max - 1) + "…";
    }

    private void setStatus(String text, int color) {
        if (statusText != null) {
            statusText.setText(text);
            statusText.setTextColor(color);
        }
    }

    private android.content.SharedPreferences getPrefs() {
        return getSharedPreferences(PREFS, MODE_PRIVATE);
    }

    private TextView label(String text, int size, int color, boolean bold) {
        TextView v = new TextView(this);
        v.setText(text);
        v.setTextSize(size);
        v.setTextColor(color);
        v.setGravity(Gravity.RIGHT);
        v.setTextDirection(View.TEXT_DIRECTION_RTL);
        v.setTypeface(Typeface.DEFAULT, bold ? Typeface.BOLD : Typeface.NORMAL);
        return v;
    }

    private EditText field(String hint, boolean password) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setTextSize(15);
        e.setTextColor(ink);
        e.setHintTextColor(0xff9aa5b5);
        e.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        e.setPadding(dp(14), 0, dp(14), 0);
        e.setSingleLine(true);
        e.setBackground(round(line, 12, 0xfff9fbfe));
        if (password) {
            e.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        }
        return e;
    }

    private Button button(String text, boolean filled) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextSize(14);
        b.setAllCaps(false);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setTextColor(filled ? Color.WHITE : ink);
        b.setPadding(dp(12), 0, dp(12), 0);
        b.setBackground(round(filled ? primary : surface, 14, filled ? primary : line));
        return b;
    }

    private LinearLayout card() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setBackground(round(surface, 20, line));
        return l;
    }

    private android.graphics.drawable.GradientDrawable round(int fill, int radius, int stroke) {
        android.graphics.drawable.GradientDrawable g = new android.graphics.drawable.GradientDrawable();
        g.setColor(fill);
        g.setCornerRadius(dp(radius));
        g.setStroke(dp(1), stroke);
        return g;
    }

    private LinearLayout.LayoutParams matchWrap(int top) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        p.topMargin = dp(top);
        return p;
    }

    private void spaceGap(LinearLayout parent, int width) {
        View gap = new View(this);
        parent.addView(gap, new LinearLayout.LayoutParams(dp(width), 1));
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
