package com.example.weddingcountdown;

import java.util.Calendar;

/**
 * Offline Hebrew-calendar conversion. Uses the traditional molad/postponement
 * calculation and works on the app's minimum Android API (14).
 */
public final class HebrewDate {
    private static final String[] MONTHS = {
        "", "תשרי", "חשוון", "כסלו", "טבת", "שבט",
        "אדר", "ניסן", "אייר", "סיוון", "תמוז", "אב", "אלול"
    };

    private HebrewDate() { }

    public static String format(Calendar source) {
        Calendar date = (Calendar) source.clone();
        int gy = date.get(Calendar.YEAR);
        int gm = date.get(Calendar.MONTH) + 1;
        int gd = date.get(Calendar.DAY_OF_MONTH);
        long jdn = gregorianToJdn(gy, gm, gd);

        int year = gy + 3760;
        while (jdn < roshHashanah(year)) year--;
        while (jdn >= roshHashanah(year + 1)) year++;

        int dayOfYear = (int)(jdn - roshHashanah(year));
        int month = 1;
        while (dayOfYear >= monthLength(year, month)) {
            dayOfYear -= monthLength(year, month);
            month++;
        }
        int day = dayOfYear + 1;
        String monthName = monthName(year, month);
        return hebrewNumber(day) + " ב" + monthName + " " + hebrewNumber(year);
    }

    private static long gregorianToJdn(int year, int month, int day) {
        int a = (14 - month) / 12;
        long y = year + 4800L - a;
        int m = month + 12 * a - 3;
        return day + (153L * m + 2) / 5 + 365L * y + y / 4
                - y / 100 + y / 400 - 32045;
    }

    private static long roshHashanah(int year) {
        return 347998L + elapsedDays(year) + delay2(year);
    }

    private static long elapsedDays(int year) {
        long months = (235L * year - 234L) / 19L;
        long parts = 12084L + 13753L * months;
        long day = 29L * months + parts / 25920L;
        if ((3L * (day + 1L)) % 7L < 3L) day++;
        return day;
    }

    private static int delay2(int year) {
        long last = elapsedDays(year - 1);
        long present = elapsedDays(year);
        long next = elapsedDays(year + 1);
        if (next - present == 356L) return 2;
        if (present - last == 382L) return 1;
        return 0;
    }

    private static boolean leap(int year) {
        return ((7 * year + 1) % 19) < 7;
    }

    private static int yearLength(int year) {
        return (int)(roshHashanah(year + 1) - roshHashanah(year));
    }

    private static int monthLength(int year, int month) {
        boolean leap = leap(year);
        int length = yearLength(year);
        switch (month) {
            case 1: return 30;
            case 2: return length % 10 == 5 ? 30 : 29;
            case 3: return length % 10 == 3 ? 29 : 30;
            case 4: return 29;
            case 5: return 30;
            case 6: return leap ? 30 : 29;
            case 7: return leap ? 29 : 30;
            case 8: return 30;
            case 9: return 29;
            case 10: return 30;
            case 11: return 29;
            case 12: return 30;
            default: return 29;
        }
    }

    private static String monthName(int year, int month) {
        if (leap(year)) {
            if (month == 6) return "אדר א׳";
            if (month == 7) return "אדר ב׳";
            if (month >= 8) return MONTHS[month - 1];
        } else if (month >= 7) {
            return MONTHS[month - 1];
        }
        return MONTHS[month];
    }

    private static String hebrewNumber(int value) {
        if (value <= 0) return String.valueOf(value);
        String[] hundreds = {"", "ק", "ר", "ש", "ת"};
        StringBuilder out = new StringBuilder();
        int n = value;
        while (n >= 400) { out.append("ת"); n -= 400; }
        if (n >= 100) { int h = n / 100; out.append(hundreds[h]); n %= 100; }
        if (n == 15) out.append("טו");
        else if (n == 16) out.append("טז");
        else {
            if (n >= 10) {
                String[] tens = {"", "י", "כ", "ל", "מ", "נ", "ס", "ע", "פ", "צ"};
                out.append(tens[n / 10]);
                n %= 10;
            }
            String[] units = {"", "א", "ב", "ג", "ד", "ה", "ו", "ז", "ח", "ט"};
            if (n > 0) out.append(units[n]);
        }
        if (out.length() == 1) return out + "׳";
        return out.substring(0, out.length() - 1) + "״" + out.substring(out.length() - 1);
    }
}