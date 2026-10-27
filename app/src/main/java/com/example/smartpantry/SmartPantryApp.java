package com.example.smartpantry;

import android.app.Application;

/** Runs once per process: creates the notification channel and (re)schedules the daily reminder. */
public class SmartPantryApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        ExpiryReminderWorker.ensureChannel(this);
        if (Prefs.alertsEnabled(this)) {
            ReminderScheduler.schedule(this, false); // KEEP: do not push back an already-scheduled run
        }
    }
}
