package com.example.smartpantry;

import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.google.android.material.materialswitch.MaterialSwitch;

/**
 * Settings: the expiry-reminder toggle. Turning it on schedules the daily WorkManager job (and asks
 * for notification permission on Android 13+); turning it off cancels the job.
 */
public class SettingsActivity extends BaseNavActivity {
    private MaterialSwitch switchAlerts;

    private final ActivityResultLauncher<String> notificationPermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (!granted) {
                    Toast.makeText(this, "Notifications are blocked, so reminders cannot be shown. "
                            + "Allow them in your phone's app settings.", Toast.LENGTH_LONG).show();
                }
            });

    @Override protected int contentLayout() { return R.layout.content_settings; }
    @Override protected int navItemId() { return R.id.nav_settings; }
    @Override protected int titleRes() { return R.string.title_settings; }

    @Override
    protected void onContentCreated(Bundle savedInstanceState) {
        switchAlerts = findViewById(R.id.switchAlerts);
        switchAlerts.setChecked(Prefs.alertsEnabled(this));       // set the state BEFORE adding the listener
        switchAlerts.setOnCheckedChangeListener((button, on) -> {
            Prefs.setAlertsEnabled(this, on);
            if (on) {
                ReminderScheduler.schedule(this, true);
                if (!NotificationPermission.granted(this)) notificationPermission.launch(NotificationPermission.PERMISSION);
            } else {
                ReminderScheduler.cancel(this);
            }
        });

        Button test = findViewById(R.id.btnTestReminder);
        test.setOnClickListener(v -> {
            if (!NotificationPermission.granted(this)) {
                notificationPermission.launch(NotificationPermission.PERMISSION);
                return;
            }
            boolean posted = ExpiryReminderWorker.checkAndNotify(this, true);
            if (!posted) {
                Toast.makeText(this, "Notifications are turned off for this app in system settings.",
                        Toast.LENGTH_LONG).show();
            }
        });
    }
}
