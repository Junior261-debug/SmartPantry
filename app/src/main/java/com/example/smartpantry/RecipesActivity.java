package com.example.smartpantry;

import android.content.Intent;
import android.os.Bundle;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import java.util.ArrayList;
import java.util.List;

/** The whole recipe collection (seeded + the user's own), each with a "can I cook it?" status. */
public class RecipesActivity extends BaseNavActivity {
    private DbHelper db;
    private RecipeListAdapter adapter;

    @Override protected int contentLayout() { return R.layout.content_recipes; }
    @Override protected int navItemId() { return R.id.nav_recipes; }
    @Override protected int titleRes() { return R.string.title_recipes; }

    @Override
    protected void onContentCreated(Bundle savedInstanceState) {
        db = new DbHelper(this);
        RecyclerView rv = findViewById(R.id.rvRecipes);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new RecipeListAdapter(recipe -> {
            Intent i = new Intent(this, RecipeDetailActivity.class);
            i.putExtra("recipe_id", recipe.id);
            startActivity(i);
        });
        rv.setAdapter(adapter);

        ExtendedFloatingActionButton add = findViewById(R.id.btnAddRecipe);
        add.setOnClickListener(v -> startActivity(new Intent(this, AddRecipeActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        List<PantryItem> pantry = db.getAllPantry();
        long today = DateUtil.today();
        List<MatchResult> rows = new ArrayList<>();
        for (Recipe r : db.getAllRecipes()) rows.add(RecipeMatcher.analyse(r, pantry, today, RecipeMatcher.SOON_DAYS));
        adapter.submit(rows);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        db.close();
    }
}
