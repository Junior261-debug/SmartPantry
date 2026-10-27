package com.example.smartpantry;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;

/**
 * Recipe detail: full ingredient list (ticked or crossed against the pantry), the method, and the
 * "Cook this" button which deducts the ingredients from the pantry. Opened with a "recipe_id" extra.
 */
public class RecipeDetailActivity extends BackActivity {
    private DbHelper db;
    private long recipeId;
    private Recipe recipe;

    private TextView tvTitle, tvMeta, tvStatus, tvSteps;
    private LinearLayout ingredientList;
    private Button btnCook, btnDelete;

    @Override protected int contentLayout() { return R.layout.content_recipe_detail; }

    @Override
    protected void onContentCreated(Bundle savedInstanceState) {
        setScreenTitle(getString(R.string.title_recipe));
        db = new DbHelper(this);
        recipeId = getIntent().getLongExtra("recipe_id", -1);

        tvTitle = findViewById(R.id.tvTitle);
        tvMeta = findViewById(R.id.tvMeta);
        tvStatus = findViewById(R.id.tvStatus);
        tvSteps = findViewById(R.id.tvSteps);
        ingredientList = findViewById(R.id.ingredientList);
        btnCook = findViewById(R.id.btnCook);
        btnDelete = findViewById(R.id.btnDeleteRecipe);

        btnCook.setOnClickListener(v -> confirmCook());
        btnDelete.setOnClickListener(v -> confirmDelete());
    }

    @Override
    protected void onResume() {
        super.onResume();
        render();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        db.close();
    }

    private void render() {
        recipe = db.getRecipe(recipeId);
        if (recipe == null) { finish(); return; }

        List<PantryItem> pantry = db.getAllPantry();
        MatchResult m = RecipeMatcher.analyse(recipe, pantry, DateUtil.today(), RecipeMatcher.SOON_DAYS);

        tvTitle.setText(recipe.name);
        tvMeta.setText(recipe.minutes + " min" + (recipe.custom ? "  •  My recipe" : ""));
        tvSteps.setText(recipe.steps);

        // status line + "Cook this" availability follow the strict rule
        if (m.isReady()) {
            tvStatus.setText(m.usesExpiring()
                    ? "You have everything – and it uses food that expires soon"
                    : "You have everything you need");
            tvStatus.setTextColor(ContextCompat.getColor(this, R.color.expiry_ok));
        } else if (m.isAlmostThere()) {
            tvStatus.setText("Almost there – missing 1 ingredient");
            tvStatus.setTextColor(ContextCompat.getColor(this, R.color.expiry_soon));
        } else {
            tvStatus.setText("Missing " + m.gaps.size() + " ingredients");
            tvStatus.setTextColor(ContextCompat.getColor(this, R.color.expiry_expired));
        }
        btnCook.setEnabled(m.isReady());
        btnDelete.setVisibility(recipe.custom ? View.VISIBLE : View.GONE);

        // ingredient rows: tick if covered, cross + what is short if not
        ingredientList.removeAllViews();
        for (RecipeIngredient ri : recipe.ingredients) {
            MatchResult.Gap gap = null;
            for (MatchResult.Gap g : m.gaps) if (g.ingredient == ri) gap = g;

            TextView row = new TextView(this);
            row.setTextAppearance(android.R.style.TextAppearance_Medium);
            int pad = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4, getResources().getDisplayMetrics());
            row.setPadding(0, pad, 0, pad);
            String line = Units.describe(ri.quantity, ri.unit) + "  " + ri.name;
            if (gap == null) {
                row.setText("✓  " + line);
                row.setTextColor(ContextCompat.getColor(this, R.color.expiry_ok));
            } else {
                row.setText("✗  " + line + (gap.absent ? "  (not in pantry)"
                        : "  (need " + Units.describe(gap.shortBy, ri.unit) + " more)"));
                row.setTextColor(ContextCompat.getColor(this, R.color.expiry_expired));
            }
            ingredientList.addView(row);
        }
    }

    private void confirmCook() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Cook " + recipe.name + "?")
                .setMessage("The ingredients will be taken out of your pantry. Items that run out are removed.")
                .setPositiveButton(R.string.cook_this, (d, w) -> cook())
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void cook() {
        CookPlanner.Plan plan = db.cook(recipe);   // one transaction: all deductions or none
        if (plan.possible) {
            Toast.makeText(this, "Enjoy your " + recipe.name + "! Pantry updated.", Toast.LENGTH_LONG).show();
            finish();
        } else {
            Toast.makeText(this, "Not enough: " + TextUtils.join(", ", plan.missing), Toast.LENGTH_LONG).show();
            render();
        }
    }

    private void confirmDelete() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Delete " + recipe.name + "?")
                .setMessage("This removes your recipe permanently.")
                .setPositiveButton(R.string.delete, (d, w) -> {
                    db.deleteCustomRecipe(recipe.id);
                    finish();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }
}
