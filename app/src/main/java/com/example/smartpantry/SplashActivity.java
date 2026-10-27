package com.example.smartpantry;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.appcompat.app.AppCompatActivity;

/** Branded opening screen shown briefly before the pantry dashboard. */
public class SplashActivity extends AppCompatActivity {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable openApp = () -> {
        startActivity(new Intent(SplashActivity.this, PantryActivity.class));
        finish();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    };

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        handler.postDelayed(openApp, 1800L);
    }

    @Override protected void onDestroy() {
        handler.removeCallbacks(openApp);
        super.onDestroy();
    }
}
