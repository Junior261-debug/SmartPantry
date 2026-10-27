package com.example.smartpantry;

import android.content.Context;
import android.content.SharedPreferences;

/** Tiny wrapper around SharedPreferences so the key names live in one place. */
public final class Prefs {
    private Prefs() {}

    public static final String NAME = "prefs";
    public static final String EXPIRY_ALERTS = "expiry_alerts";

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(NAME, Context.MODE_PRIVATE);
    }

    /** The Settings toggle. On by default. */
    public static boolean alertsEnabled(Context c) {
        return prefs(c).getBoolean(EXPIRY_ALERTS, true);
    }

    public static void setAlertsEnabled(Context c, boolean on) {
        prefs(c).edit().putBoolean(EXPIRY_ALERTS, on).apply();
    }

    private static final String ASKED_NOTIFICATIONS = "asked_notifications";

    /** True once we have shown the Android 13 notification permission prompt (we only ask once on launch). */
    public static boolean askedForNotifications(Context c) {
        return prefs(c).getBoolean(ASKED_NOTIFICATIONS, false);
    }

    public static void setAskedForNotifications(Context c) {
        prefs(c).edit().putBoolean(ASKED_NOTIFICATIONS, true).apply();
    }
}
