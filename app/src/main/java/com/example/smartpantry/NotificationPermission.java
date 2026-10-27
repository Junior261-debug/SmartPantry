package com.example.smartpantry;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.content.ContextCompat;

/** Android 13+ needs the user to allow notifications; older versions allow them by default. */
public final class NotificationPermission {
    private NotificationPermission() {}

    public static final String PERMISSION = "android.permission.POST_NOTIFICATIONS";

    /** True if we may post notifications without asking (always true below Android 13). */
    public static boolean granted(Context c) {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
                || ContextCompat.checkSelfPermission(c, PERMISSION) == PackageManager.PERMISSION_GRANTED;
    }
}
