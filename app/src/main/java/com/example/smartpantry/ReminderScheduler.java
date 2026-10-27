package com.example.smartpantry;

import android.content.Context;

import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

/** Schedules / cancels the daily expiry reminder with WorkManager. */
public final class ReminderScheduler {
    private ReminderScheduler() {}

    private static final String WORK_NAME = "expiry_reminder";
    private static final int HOUR_OF_DAY = 9; // reminder time: 09:00 local

    /**
     * @param replace true when the user just switched the toggle on (restart the 24h cycle);
     *                false at app start (keep the existing schedule if there is one).
     */
    public static void schedule(Context context, boolean replace) {
        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(ExpiryReminderWorker.class, 24, TimeUnit.HOURS)
                .setInitialDelay(millisUntilNextRun(System.currentTimeMillis()), TimeUnit.MILLISECONDS)
                .build();
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(WORK_NAME,
                replace ? ExistingPeriodicWorkPolicy.UPDATE : ExistingPeriodicWorkPolicy.KEEP, request);
    }

    public static void cancel(Context context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME);
    }

    /** Milliseconds from {@code now} until the next 09:00 (today if still ahead, otherwise tomorrow). */
    static long millisUntilNextRun(long now) {
        Calendar next = Calendar.getInstance();
        next.setTimeInMillis(now);
        next.set(Calendar.HOUR_OF_DAY, HOUR_OF_DAY);
        next.set(Calendar.MINUTE, 0);
        next.set(Calendar.SECOND, 0);
        next.set(Calendar.MILLISECOND, 0);
        if (next.getTimeInMillis() <= now) next.add(Calendar.DAY_OF_YEAR, 1);
        return next.getTimeInMillis() - now;
    }
}
