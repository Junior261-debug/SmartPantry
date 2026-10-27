package com.example.smartpantry;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.List;

/**
 * Suggested Recipes: runs the strict-matching rule against the current pantry.
 *  - "Ready to cook": every ingredient present in enough quantity (the strict list).
 *  - "Almost there": missing exactly one ingredient, kept clearly separate (bonus feature).
 * Both lists are ranked so recipes that use up soon-to-expire food come first, and the filter
 * chips (quick meals, uses expiring food, my recipes) narrow them down.
 */
public class SuggestedRecipesActivity extends BaseNavActivity {
    private DbHelper db;
    private SuggestionAdapter adapter;
    private RecyclerView rv;
    private View emptyState;
    private Chip chipQuick, chipExpiring, chipMine;

    @Override protected int contentLayout() { return R.layout.content_suggestions; }
    @Override protected int navItemId() { return R.id.nav_suggestions; }
    @Override protected int titleRes() { return R.string.title_suggestions; }

    @Override
    protected void onContentCreated(Bundle savedInstanceState) {
        db = new DbHelper(this);
        rv = findViewById(R.id.rvSuggestions);
        emptyState = findViewById(R.id.emptyState);
        chipQuick = findViewById(R.id.chipQuick);
        chipExpiring = findViewById(R.id.chipExpiring);
        chipMine = findViewById(R.id.chipMine);

        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SuggestionAdapter(recipe -> {
            Intent i = new Intent(this, RecipeDetailActivity.class);
            i.putExtra("recipe_id", recipe.id);
            startActivity(i);
        });
        rv.setAdapter(adapter);

        chipQuick.setOnCheckedChangeListener((b, checked) -> render());
        chipExpiring.setOnCheckedChangeListener((b, checked) -> render());
        chipMine.setOnCheckedChangeListener((b, checked) -> render());
    }

    /** The pantry may have changed on another screen (or after "Cook this"), so recompute each time. */
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
        List<Recipe> recipes = db.getAllRecipes();
        List<PantryItem> pantry = db.getAllPantry();
        long today = DateUtil.today();

        // 1. the matching itself (already ranked expiry-first)
        List<MatchResult> readyAll = RecipeMatcher.ready(recipes, pantry, today, RecipeMatcher.SOON_DAYS);
        List<MatchResult> almostAll = RecipeMatcher.almostThere(recipes, pantry, today, RecipeMatcher.SOON_DAYS);

        // 2. the filter chips
        boolean quick = chipQuick.isChecked(), expiring = chipExpiring.isChecked(), mine = chipMine.isChecked();
        List<MatchResult> ready = Filters.suggestions(readyAll, quick, expiring, mine);
        List<MatchResult> almost = Filters.suggestions(almostAll, quick, expiring, mine);

        // 3. nothing at all: friendly feedback instead of a blank screen
        if (readyAll.isEmpty() && almostAll.isEmpty()) {
            show(R.string.empty_suggest_title, R.string.empty_suggest_msg);
            return;
        }
        if (ready.isEmpty() && almost.isEmpty()) {
            show(R.string.empty_filter_title, R.string.empty_filter_msg);
            return;
        }
        UiUtil.hide(emptyState);
        rv.setVisibility(View.VISIBLE);

        // 4. build the rows: strict list first, then the clearly separate "Almost there" list
        List<SuggestionAdapter.Row> rows = new ArrayList<>();
        rows.add(SuggestionAdapter.Row.header("Ready to cook (" + ready.size() + ")",
                "You have every ingredient. Recipes that use food expiring soon come first."));
        if (ready.isEmpty()) {
            rows.add(SuggestionAdapter.Row.message(readyAll.isEmpty()
                    ? getString(R.string.empty_suggest_title) + ". " + getString(R.string.empty_suggest_msg)
                    : "None of your ready recipes match these filters."));
        }
        for (MatchResult m : ready) rows.add(SuggestionAdapter.Row.recipe(m));

        if (!almost.isEmpty()) {
            rows.add(SuggestionAdapter.Row.header("Almost there (" + almost.size() + ")",
                    "Missing just one ingredient. Not part of the ready list above."));
            for (MatchResult m : almost) rows.add(SuggestionAdapter.Row.recipe(m));
        }
        adapter.submit(rows);
    }

    private void show(int titleRes, int msgRes) {
        adapter.submit(new ArrayList<>());
        rv.setVisibility(View.GONE);
        UiUtil.showEmpty(emptyState, R.drawable.ic_empty_recipes, titleRes, msgRes);
    }
}
