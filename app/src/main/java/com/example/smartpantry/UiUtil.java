package com.example.smartpantry;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

/** Small helpers shared by the screens. */
public final class UiUtil {
    private UiUtil() {}

    /** Runs {@code r} whenever the text of {@code e} changes. */
    public static void afterTextChanged(EditText e, Runnable r) {
        e.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void afterTextChanged(Editable s) { r.run(); }
        });
    }

    /** Fills and shows the shared empty-state view (illustration + title + message). */
    public static void showEmpty(View emptyRoot, int imageRes, int titleRes, int messageRes) {
        ((ImageView) emptyRoot.findViewById(R.id.emptyImage)).setImageResource(imageRes);
        ((TextView) emptyRoot.findViewById(R.id.emptyTitle)).setText(titleRes);
        ((TextView) emptyRoot.findViewById(R.id.emptyMessage)).setText(messageRes);
        emptyRoot.setVisibility(View.VISIBLE);
    }

    public static void hide(View v) { v.setVisibility(View.GONE); }
}
