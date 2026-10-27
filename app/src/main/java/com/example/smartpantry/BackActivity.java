package com.example.smartpantry;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;

/** Shell for screens opened from a tab (add/edit, recipe detail): toolbar with a back arrow. */
public abstract class BackActivity extends AppCompatActivity {

    protected abstract int contentLayout();
    protected abstract void onContentCreated(Bundle savedInstanceState);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_back);
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_back);
        toolbar.setNavigationOnClickListener(v -> finish());
        getLayoutInflater().inflate(contentLayout(), findViewById(R.id.content), true);
        onContentCreated(savedInstanceState);
    }

    protected void setScreenTitle(CharSequence title) {
        ((MaterialToolbar) findViewById(R.id.toolbar)).setTitle(title);
    }
}
