package com.example.smartpantry;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;

/**
 * Shared shell for the four top-level screens (Pantry, Suggested, Recipes, Settings):
 * a toolbar, the screen's own content, and the bottom navigation bar.
 * Subclasses only describe their content layout, tab and title.
 */
public abstract class BaseNavActivity extends AppCompatActivity {

    protected abstract int contentLayout();
    protected abstract int navItemId();
    protected abstract int titleRes();
    /** Called once the content layout is inflated; look up views and set listeners here. */
    protected abstract void onContentCreated(Bundle savedInstanceState);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_base);
        ((MaterialToolbar) findViewById(R.id.toolbar)).setTitle(titleRes());
        getLayoutInflater().inflate(contentLayout(), findViewById(R.id.content), true);

        BottomNavigationView nav = findViewById(R.id.bottom_nav);
        nav.setSelectedItemId(navItemId());
        nav.setOnItemSelectedListener(item -> {
            navigateTo(item.getItemId());
            return true;
        });
        onContentCreated(savedInstanceState);
    }

    private void navigateTo(int itemId) {
        if (itemId == navItemId()) return;
        Class<?> target;
        if (itemId == R.id.nav_pantry) target = PantryActivity.class;
        else if (itemId == R.id.nav_suggestions) target = SuggestedRecipesActivity.class;
        else if (itemId == R.id.nav_recipes) target = RecipesActivity.class;
        else target = SettingsActivity.class;

        Intent i = new Intent(this, target);
        if (target == PantryActivity.class) {
            // Pantry is the root: return to it and drop everything above it.
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(i);
        } else {
            startActivity(i);
            if (!(this instanceof PantryActivity)) finish(); // tabs replace each other; Back returns to Pantry
        }
        overridePendingTransition(0, 0);
    }
}
