package com.example.smartpantry;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Calendar;

/**
 * Create / Update screen for one pantry item. Opened with no extras to add, or with an "id" extra
 * (passed by an Intent from the pantry list) to edit. Every field is validated before saving.
 */
public class AddEditIngredientActivity extends BackActivity {
    private DbHelper db;
    private long itemId = -1;           // -1 = adding a new item
    private String expiry;              // "yyyy-MM-dd" or null

    private TextInputLayout tilName, tilQty, tilUnit, tilExpiry;
    private TextInputEditText etName, etQty, etExpiry;
    private AutoCompleteTextView acUnit;

    @Override protected int contentLayout() { return R.layout.content_add_edit; }

    @Override
    protected void onContentCreated(Bundle savedInstanceState) {
        db = new DbHelper(this);
        tilName = findViewById(R.id.til_name);
        tilQty = findViewById(R.id.til_qty);
        tilUnit = findViewById(R.id.til_unit);
        tilExpiry = findViewById(R.id.til_expiry);
        etName = findViewById(R.id.et_name);
        etQty = findViewById(R.id.et_qty);
        etExpiry = findViewById(R.id.et_expiry);
        acUnit = findViewById(R.id.ac_unit);

        acUnit.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, Units.CHOICES));

        itemId = getIntent().getLongExtra("id", -1);   // read the id passed by the Intent
        if (itemId > 0) {
            setScreenTitle(getString(R.string.title_edit_item));
            PantryItem p = db.getPantry(itemId);
            if (p == null) { finish(); return; }
            etName.setText(p.name);
            etQty.setText(Units.format(p.quantity));
            acUnit.setText(Units.canonical(p.unit), false);
            expiry = DateUtil.parseEpochDay(p.expiry) == DateUtil.NO_EXPIRY ? null : p.expiry;
        } else {
            setScreenTitle(getString(R.string.title_add_item));
            acUnit.setText("pcs", false);
        }
        showExpiry();

        // clear an error as soon as the user starts fixing the field
        UiUtil.afterTextChanged(etName, () -> tilName.setError(null));
        UiUtil.afterTextChanged(etQty, () -> tilQty.setError(null));
        UiUtil.afterTextChanged(acUnit, () -> tilUnit.setError(null));

        etExpiry.setOnClickListener(v -> pickDate());
        tilExpiry.setEndIconOnClickListener(v -> pickDate());
        Button clear = findViewById(R.id.btnClearDate);
        clear.setOnClickListener(v -> { expiry = null; showExpiry(); });
        Button save = findViewById(R.id.btnSave);
        save.setOnClickListener(v -> save());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (db != null) db.close();
    }

    private void showExpiry() {
        etExpiry.setText(expiry == null ? "" : expiry);
    }

    private void pickDate() {
        Calendar c = Calendar.getInstance();
        long e = DateUtil.parseEpochDay(expiry);
        if (e != DateUtil.NO_EXPIRY) {
            String[] p = expiry.split("-");
            c.set(Integer.parseInt(p[0]), Integer.parseInt(p[1]) - 1, Integer.parseInt(p[2]));
        }
        new DatePickerDialog(this, (view, y, m, d) -> {
            expiry = DateUtil.format(y, m + 1, d);
            showExpiry();
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }

    /** Validates every field, shows an error under each bad one, and saves only if all are fine. */
    private void save() {
        String name = Validation.name(text(etName));
        Double qty = Validation.quantity(text(etQty));
        String unit = acUnit.getText().toString();
        boolean unitOk = Validation.unit(unit);

        tilName.setError(name == null ? getString(R.string.error_name) : null);
        tilQty.setError(qty == null ? getString(R.string.error_quantity) : null);
        tilUnit.setError(unitOk ? null : getString(R.string.error_unit));
        if (name == null || qty == null || !unitOk) return;

        PantryItem item = new PantryItem(itemId > 0 ? itemId : 0, name, qty, Units.canonical(unit), expiry);
        if (itemId > 0) db.updatePantry(item); else db.addPantry(item);
        Toast.makeText(this, name + " saved", Toast.LENGTH_SHORT).show();
        finish();
    }

    private static String text(TextInputEditText e) {
        return e.getText() == null ? "" : e.getText().toString();
    }
}
