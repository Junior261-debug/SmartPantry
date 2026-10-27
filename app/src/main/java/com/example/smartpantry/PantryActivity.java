package com.example.smartpantry;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

/**
 * Launcher screen: the pantry list (the "Read" in CRUD) with a search box, plus navigation to
 * Add / Edit / Delete. Part of the bottom-navigation shell.
 */
public class PantryActivity extends BaseNavActivity {
    private DbHelper db;
    private PantryAdapter adapter;
    private RecyclerView rv;
    private View emptyState;
    private TextInputEditText etSearch;
    private List<PantryItem> allItems = new ArrayList<>();

    /** Android 13+: asks once for permission to show the daily expiry reminder. */
    private final ActivityResultLauncher<String> notificationPermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> { });

    @Override protected int contentLayout() { return R.layout.content_pantry; }
    @Override protected int navItemId() { return R.id.nav_pantry; }
    @Override protected int titleRes() { return R.string.title_pantry; }

    @Override
    protected void onContentCreated(Bundle savedInstanceState) {
        db = new DbHelper(this);
        rv = findViewById(R.id.rvPantry);
        emptyState = findViewById(R.id.emptyState);
        etSearch = findViewById(R.id.et_search);

        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PantryAdapter(new ArrayList<>(), true, new PantryAdapter.Listener() {
            @Override public void onEdit(PantryItem item) {
                Intent i = new Intent(PantryActivity.this, AddEditIngredientActivity.class);
                i.putExtra("id", item.id); // pass data to the next screen via the Intent
                startActivity(i);
            }
            @Override public void onDelete(PantryItem item) { confirmDelete(item); }
        });
        rv.setAdapter(adapter);

        ExtendedFloatingActionButton add = findViewById(R.id.btnAdd);
        add.setOnClickListener(v -> startActivity(new Intent(this, AddEditIngredientActivity.class)));

        // Search: re-filter the list on every keystroke
        UiUtil.afterTextChanged(etSearch, this::render);

        askForNotificationPermissionOnce();
    }

    /** Refresh in onResume so edits made on other screens appear when we come back. */
    @Override
    protected void onResume() {
        super.onResume();
        allItems = db.getAllPantry();
        render();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        db.close();
    }

    /** Applies the search text and shows either the list or the right empty state. */
    private void render() {
        String query = etSearch.getText() == null ? "" : etSearch.getText().toString();
        List<PantryItem> shown = Filters.pantry(allItems, query);
        adapter.submit(shown, Prefs.alertsEnabled(this));

        if (allItems.isEmpty()) {
            UiUtil.showEmpty(emptyState, R.drawable.ic_empty_pantry, R.string.empty_pantry_title, R.string.empty_pantry_msg);
        } else if (shown.isEmpty()) {
            UiUtil.showEmpty(emptyState, R.drawable.ic_empty_pantry, R.string.empty_search_title, R.string.empty_search_msg);
        } else {
            UiUtil.hide(emptyState);
        }
    }

    private void confirmDelete(PantryItem item) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Delete " + item.name + "?")
                .setMessage("This removes it from your pantry.")
                .setPositiveButton(R.string.delete, (d, w) -> {
                    db.deletePantry(item.id);
                    allItems = db.getAllPantry();
                    render();
                    Snackbar.make(rv, item.name + " deleted", Snackbar.LENGTH_SHORT).show();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void askForNotificationPermissionOnce() {
        if (Prefs.alertsEnabled(this) && !NotificationPermission.granted(this) && !Prefs.askedForNotifications(this)) {
            Prefs.setAskedForNotifications(this);
            notificationPermission.launch(NotificationPermission.PERMISSION);
        }
    }
}
