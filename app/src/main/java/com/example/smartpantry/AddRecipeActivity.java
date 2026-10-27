package com.example.smartpantry;

import android.database.SQLException;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;

/**
 * Lets the user add their own recipe: name, time, any number of ingredient rows and the method.
 * Custom recipes are stored in the same tables as the seeded ones, so they take part in matching,
 * "Almost there", ranking and "Cook this" exactly like the built-in recipes.
 */
public class AddRecipeActivity extends BackActivity {
    private DbHelper db;
    private TextInputLayout tilName, tilMinutes, tilSteps;
    private TextInputEditText etName, etMinutes, etSteps;
    private TextView tvIngredientsError;
    private LinearLayout rows;

    @Override protected int contentLayout() { return R.layout.content_add_recipe; }

    @Override
    protected void onContentCreated(Bundle savedInstanceState) {
        setScreenTitle(getString(R.string.title_add_recipe));
        db = new DbHelper(this);
        tilName = findViewById(R.id.til_rname);
        tilMinutes = findViewById(R.id.til_minutes);
        tilSteps = findViewById(R.id.til_steps);
        etName = findViewById(R.id.et_rname);
        etMinutes = findViewById(R.id.et_minutes);
        etSteps = findViewById(R.id.et_steps);
        tvIngredientsError = findViewById(R.id.tvIngredientsError);
        rows = findViewById(R.id.ingredientRows);

        UiUtil.afterTextChanged(etName, () -> tilName.setError(null));
        UiUtil.afterTextChanged(etMinutes, () -> tilMinutes.setError(null));
        UiUtil.afterTextChanged(etSteps, () -> tilSteps.setError(null));

        addRow();
        addRow();
        Button addRow = findViewById(R.id.btnAddRow);
        addRow.setOnClickListener(v -> addRow());
        Button save = findViewById(R.id.btnSaveRecipe);
        save.setOnClickListener(v -> save());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        db.close();
    }

    /** Adds one empty ingredient row (name / quantity / unit / remove button). */
    private void addRow() {
        View row = LayoutInflater.from(this).inflate(R.layout.row_ingredient_input, rows, false);
        AutoCompleteTextView unit = row.findViewById(R.id.ac_ing_unit);
        unit.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, Units.CHOICES));
        unit.setText("pcs", false);
        row.findViewById(R.id.btnRemoveRow).setOnClickListener(v -> {
            if (rows.getChildCount() > 1) rows.removeView(row);   // always keep one row
        });
        rows.addView(row);
    }

    private void save() {
        // --- basic fields ---
        String name = Validation.name(text(etName));
        Integer minutes = Validation.minutes(text(etMinutes));
        String steps = text(etSteps).trim();

        tilName.setError(name == null ? getString(R.string.error_recipe_name) : null);
        if (name != null && nameExists(name)) {
            tilName.setError(getString(R.string.error_recipe_duplicate));
            name = null;
        }
        tilMinutes.setError(minutes == null ? getString(R.string.error_minutes) : null);
        tilSteps.setError(steps.isEmpty() ? getString(R.string.error_steps) : null);

        // --- ingredient rows (a completely empty row is ignored) ---
        List<RecipeIngredient> ingredients = new ArrayList<>();
        boolean rowsOk = true;
        for (int i = 0; i < rows.getChildCount(); i++) {
            View row = rows.getChildAt(i);
            TextInputLayout tilN = row.findViewById(R.id.til_ing_name);
            TextInputLayout tilQ = row.findViewById(R.id.til_ing_qty);
            TextInputLayout tilU = row.findViewById(R.id.til_ing_unit);
            String rawName = text((TextInputEditText) row.findViewById(R.id.et_ing_name));
            String rawQty = text((TextInputEditText) row.findViewById(R.id.et_ing_qty));
            String rawUnit = ((AutoCompleteTextView) row.findViewById(R.id.ac_ing_unit)).getText().toString();
            tilN.setError(null); tilQ.setError(null); tilU.setError(null);

            if (rawName.trim().isEmpty() && rawQty.trim().isEmpty()) continue;

            String n = Validation.name(rawName);
            Double q = Validation.quantity(rawQty);
            boolean u = Validation.unit(rawUnit);
            if (n == null) tilN.setError(getString(R.string.error_name));
            if (q == null) tilQ.setError(getString(R.string.error_quantity));
            if (!u) tilU.setError(getString(R.string.error_unit));
            if (n == null || q == null || !u) { rowsOk = false; continue; }
            ingredients.add(new RecipeIngredient(n, q, Units.canonical(rawUnit)));
        }
        boolean noIngredients = ingredients.isEmpty();
        tvIngredientsError.setVisibility(noIngredients && rowsOk ? View.VISIBLE : View.GONE);
        if (noIngredients && rowsOk) tvIngredientsError.setText(R.string.error_no_ingredients);

        if (name == null || minutes == null || steps.isEmpty() || !rowsOk || noIngredients) return;

        Recipe r = new Recipe(0, name, steps);
        r.minutes = minutes;
        r.custom = true;
        r.ingredients.addAll(ingredients);
        try {
            db.addCustomRecipe(r);
        } catch (SQLException e) {
            Toast.makeText(this, "Could not save the recipe. Please try again.", Toast.LENGTH_LONG).show();
            return;
        }
        Toast.makeText(this, name + " added to your recipes", Toast.LENGTH_SHORT).show();
        finish();
    }

    /** True if a recipe with this name (ignoring case and spacing) already exists. */
    private boolean nameExists(String name) {
        for (Recipe r : db.getAllRecipes()) if (r.name.equalsIgnoreCase(name)) return true;
        return false;
    }

    private static String text(TextInputEditText e) {
        return e.getText() == null ? "" : e.getText().toString();
    }
}
