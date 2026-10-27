package com.example.smartpantry;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Background job (WorkManager) that runs once a day. If the Settings toggle is on and something
 * in the pantry expires within {@link RecipeMatcher#SOON_DAYS} days, it posts one notification.
 */
public class ExpiryReminderWorker extends Worker {
    static final String CHANNEL_ID = "expiry_reminders";
    private static final int NOTIFICATION_ID = 1001;

    public ExpiryReminderWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context ctx = getApplicationContext();
        if (Prefs.alertsEnabled(ctx)) checkAndNotify(ctx, false); // toggle may have been switched off since scheduling
        return Result.success();
    }

    /** Creates the channel on Android 8+ (no-op below, and when it already exists). */
    public static void ensureChannel(Context ctx) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationChannel ch = new NotificationChannel(CHANNEL_ID,
                ctx.getString(R.string.notification_channel), NotificationManager.IMPORTANCE_DEFAULT);
        ch.setDescription(ctx.getString(R.string.notification_channel_desc));
        NotificationManager nm = ctx.getSystemService(NotificationManager.class);
        if (nm != null) nm.createNotificationChannel(ch);
    }

    /**
     * Looks at the pantry and posts the reminder.
     *
     * @param force true for the "send a test" button: also posts a message when nothing is expiring
     * @return true if a notification was posted
     */
    @SuppressLint("MissingPermission") // areNotificationsEnabled() below covers the Android 13 permission
    public static boolean checkAndNotify(Context ctx, boolean force) {
        NotificationManagerCompat nm = NotificationManagerCompat.from(ctx);
        if (!nm.areNotificationsEnabled()) return false;
        ensureChannel(ctx);

        List<PantryItem> pantry;
        DbHelper db = new DbHelper(ctx);
        try { pantry = db.getAllPantry(); } finally { db.close(); }

        long today = DateUtil.today();
        List<PantryItem> expiring = new ArrayList<>();
        for (PantryItem p : pantry) {
            Long days = DateUtil.daysUntil(p.expiry, today);
            if (days != null && days <= RecipeMatcher.SOON_DAYS) expiring.add(p);
        }
        Collections.sort(expiring, (a, b) -> Long.compare(DateUtil.parseEpochDay(a.expiry), DateUtil.parseEpochDay(b.expiry)));

        String title, text;
        if (expiring.isEmpty()) {
            if (!force) return false;
            title = "Nothing is expiring soon";
            text = "Your pantry looks fresh. Reminders are working.";
        } else {
            title = expiring.size() == 1 ? "1 item needs using up" : expiring.size() + " items need using up";
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < expiring.size() && i < 4; i++) {
                PantryItem p = expiring.get(i);
                if (i > 0) sb.append(", ");
                sb.append(p.name).append(" (").append(shortWhen(DateUtil.daysUntil(p.expiry, today))).append(")");
            }
            if (expiring.size() > 4) sb.append(" and ").append(expiring.size() - 4).append(" more");
            text = sb + ". Tap to see recipes that use them.";
        }

        Intent open = new Intent(ctx, SuggestedRecipesActivity.class);
        PendingIntent pi = PendingIntent.getActivity(ctx, 0, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        NotificationCompat.Builder b = new NotificationCompat.Builder(ctx, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(text))
                .setContentIntent(pi)
                .setAutoCancel(true);
        nm.notify(NOTIFICATION_ID, b.build());
        return true;
    }

    private static String shortWhen(Long days) {
        if (days == null) return "?";
        if (days < 0) return "expired";
        if (days == 0) return "today";
        if (days == 1) return "tomorrow";
        return "in " + days + " days";
    }
}
