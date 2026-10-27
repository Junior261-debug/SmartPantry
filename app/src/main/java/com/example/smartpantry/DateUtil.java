package com.example.smartpantry;

import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Date helpers for "yyyy-MM-dd" expiry strings. Works in whole days since the epoch so there is
 * no time-of-day or daylight-saving drift (and no java.time, which needs API 26).
 */
public final class DateUtil {
    private DateUtil() {}

    /** Value for "no expiry known"; far enough away that it never counts as soon. */
    public static final long NO_EXPIRY = Long.MAX_VALUE;

    /** Days since 1970-01-01 for a "yyyy-MM-dd" string, or {@link #NO_EXPIRY} if blank/invalid. */
    public static long parseEpochDay(String ymd) {
        if (ymd == null) return NO_EXPIRY;
        String[] p = ymd.trim().split("-");
        if (p.length != 3) return NO_EXPIRY;
        try {
            return epochDay(Integer.parseInt(p[0]), Integer.parseInt(p[1]), Integer.parseInt(p[2]));
        } catch (NumberFormatException e) {
            return NO_EXPIRY;
        }
    }

    public static long epochDay(int year, int month, int day) {
        Calendar c = Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.US);
        c.clear();
        c.set(year, month - 1, day);
        return Math.floorDiv(c.getTimeInMillis(), 86_400_000L);
    }

    /** Today's date in the device's time zone, as an epoch day. */
    public static long today() {
        Calendar now = Calendar.getInstance();
        return epochDay(now.get(Calendar.YEAR), now.get(Calendar.MONTH) + 1, now.get(Calendar.DAY_OF_MONTH));
    }

    /** Whole days from {@code today} until expiry (negative = already expired); null if unknown. */
    public static Long daysUntil(String expiry, long today) {
        long e = parseEpochDay(expiry);
        return e == NO_EXPIRY ? null : e - today;
    }

    public static String format(int year, int month, int day) {
        return String.format(Locale.US, "%04d-%02d-%02d", year, month, day);
    }

    /** Inverse of {@link #parseEpochDay}: epoch day -> "yyyy-MM-dd". */
    public static String formatEpochDay(long epochDay) {
        Calendar c = Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.US);
        c.setTimeInMillis(epochDay * 86_400_000L);
        return format(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH));
    }

    /** Human wording for a days-until-expiry value: "Expired 2 days ago", "Expires tomorrow"... */
    public static String relative(long days) {
        if (days < -1) return "Expired " + (-days) + " days ago";
        if (days == -1) return "Expired yesterday";
        if (days == 0) return "Expires today";
        if (days == 1) return "Expires tomorrow";
        return "Expires in " + days + " days";
    }
}
