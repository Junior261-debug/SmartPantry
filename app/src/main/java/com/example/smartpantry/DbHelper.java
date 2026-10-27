package com.example.smartpantry;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

/** SQLite persistence: pantry CRUD, recipes (seeded + user-created) and the "cook" transaction. */
public class DbHelper extends SQLiteOpenHelper {
    private static final String DB_NAME = "pantry.db";

    public DbHelper(Context c) { super(c, DB_NAME, null, Schema.VERSION); }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        for (String sql : Schema.create()) db.execSQL(sql);
        seed(db); // pre-load recipes on first run
    }

    /**
     * Real migration: runs only the steps between the installed version and the new one, and
     * keeps all user data. (SQLiteOpenHelper wraps this in a transaction, so a failure rolls back.)
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        for (String sql : Schema.upgrade(oldVersion, newVersion)) db.execSQL(sql);
        // Backfill any missing bundled recipes on existing installs without duplicating
        // recipes or touching custom recipes/pantry data.
        seed(db);
    }

    // ---------- PANTRY CRUD ----------
    public long addPantry(PantryItem p) {
        return getWritableDatabase().insert("pantry", null, values(p));
    }

    public int updatePantry(PantryItem p) {
        return getWritableDatabase().update("pantry", values(p), "id=?", new String[]{String.valueOf(p.id)});
    }

    public int deletePantry(long id) {
        return getWritableDatabase().delete("pantry", "id=?", new String[]{String.valueOf(id)});
    }

    public PantryItem getPantry(long id) {
        try (Cursor c = getReadableDatabase().query("pantry", null, "id=?",
                new String[]{String.valueOf(id)}, null, null, null)) {
            return c.moveToFirst() ? fromCursor(c) : null;
        }
    }

    public List<PantryItem> getAllPantry() {
        List<PantryItem> list = new ArrayList<>();
        try (Cursor c = getReadableDatabase().query("pantry", null, null, null, null, null, "name COLLATE NOCASE")) {
            while (c.moveToNext()) list.add(fromCursor(c));
        }
        return list;
    }

    private ContentValues values(PantryItem p) {
        ContentValues v = new ContentValues();
        v.put("name", p.name); v.put("quantity", p.quantity);
        v.put("unit", p.unit); v.put("expiry", p.expiry);
        return v;
    }

    private PantryItem fromCursor(Cursor c) {
        return new PantryItem(c.getLong(c.getColumnIndexOrThrow("id")),
                c.getString(c.getColumnIndexOrThrow("name")),
                c.getDouble(c.getColumnIndexOrThrow("quantity")),
                c.getString(c.getColumnIndexOrThrow("unit")),
                c.getString(c.getColumnIndexOrThrow("expiry")));
    }

    // ---------- COOK THIS ----------
    /**
     * Deducts the recipe's quantities from the pantry (soonest-expiring stock first) and removes
     * rows that reach zero, all in one transaction. If the pantry cannot cover the recipe,
     * nothing is changed and the returned plan has {@code possible == false}.
     */
    public CookPlanner.Plan cook(Recipe recipe) {
        CookPlanner.Plan plan = CookPlanner.plan(recipe, getAllPantry());
        if (!plan.possible) return plan;
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            for (CookPlanner.Change ch : plan.changes) {
                String[] id = {String.valueOf(ch.itemId)};
                if (ch.delete) {
                    db.delete("pantry", "id=?", id);
                } else {
                    ContentValues v = new ContentValues();
                    v.put("quantity", ch.newQuantity);
                    db.update("pantry", v, "id=?", id);
                }
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        return plan;
    }

    // ---------- RECIPES ----------
    public List<Recipe> getAllRecipes() {
        List<Recipe> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor c = db.query("recipes", null, null, null, null, null, "name COLLATE NOCASE")) {
            while (c.moveToNext()) {
                Recipe r = recipeFromCursor(c);
                loadIngredients(db, r);
                list.add(r);
            }
        }
        return list;
    }

    public Recipe getRecipe(long id) {
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor c = db.query("recipes", null, "id=?", new String[]{String.valueOf(id)}, null, null, null)) {
            if (!c.moveToFirst()) return null;
            Recipe r = recipeFromCursor(c);
            loadIngredients(db, r);
            return r;
        }
    }

    private Recipe recipeFromCursor(Cursor c) {
        Recipe r = new Recipe(c.getLong(c.getColumnIndexOrThrow("id")),
                c.getString(c.getColumnIndexOrThrow("name")),
                c.getString(c.getColumnIndexOrThrow("steps")));
        r.minutes = c.getInt(c.getColumnIndexOrThrow("minutes"));
        r.custom = c.getInt(c.getColumnIndexOrThrow("is_custom")) == 1;
        return r;
    }

    private void loadIngredients(SQLiteDatabase db, Recipe r) {
        try (Cursor c = db.query("recipe_ingredients", null, "recipe_id=?",
                new String[]{String.valueOf(r.id)}, null, null, "id")) {
            while (c.moveToNext()) {
                r.ingredients.add(new RecipeIngredient(c.getString(c.getColumnIndexOrThrow("name")),
                        c.getDouble(c.getColumnIndexOrThrow("quantity")),
                        c.getString(c.getColumnIndexOrThrow("unit"))));
            }
        }
    }

    /** Saves a user-created recipe (and its ingredients) and returns its new id. */
    public long addCustomRecipe(Recipe r) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            long id = insertRecipe(db, r.name, r.steps, r.minutes, true, r.ingredients);
            db.setTransactionSuccessful();
            return id;
        } finally {
            db.endTransaction();
        }
    }

    /** Deletes a user-created recipe. Seeded recipes are protected. Returns true if deleted. */
    public boolean deleteCustomRecipe(long id) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            String[] arg = {String.valueOf(id)};
            // Foreign keys are enforced, so children go first. The sub-query keeps seeded recipes safe.
            db.delete("recipe_ingredients", "recipe_id=? AND recipe_id IN (SELECT id FROM recipes WHERE is_custom=1)", arg);
            boolean deleted = db.delete("recipes", "id=? AND is_custom=1", arg) > 0;
            db.setTransactionSuccessful();
            return deleted;
        } finally {
            db.endTransaction();
        }
    }

    private static long insertRecipe(SQLiteDatabase db, String name, String steps, int minutes,
                                     boolean custom, List<RecipeIngredient> ings) {
        ContentValues v = new ContentValues();
        v.put("name", name); v.put("steps", steps);
        v.put("minutes", minutes); v.put("is_custom", custom ? 1 : 0);
        long rid = db.insertOrThrow("recipes", null, v);
        for (RecipeIngredient ri : ings) {
            ContentValues iv = new ContentValues();
            iv.put("recipe_id", rid); iv.put("name", ri.name);
            iv.put("quantity", ri.quantity); iv.put("unit", ri.unit);
            db.insertOrThrow("recipe_ingredients", null, iv);
        }
        return rid;
    }

    // ---------- SEED DATA ----------
    /** ingredients format: "name:quantity:unit". Cooking time comes from {@link Schema#SEED_MINUTES}. */
    private void addSeed(SQLiteDatabase db, String name, String steps, String... ings) {
        // Seed only if this bundled recipe is not already present.
        try (Cursor existing = db.query("recipes", new String[]{"id"}, "name=? AND is_custom=0",
                new String[]{name}, null, null, null, "1")) {
            if (existing.moveToFirst()) return;
        }
        List<RecipeIngredient> list = new ArrayList<>();
        for (String s : ings) {
            String[] p = s.split(":");
            list.add(new RecipeIngredient(p[0], Double.parseDouble(p[1]), p[2]));
        }
        Integer minutes = Schema.SEED_MINUTES.get(name);
        insertRecipe(db, name, steps, minutes != null ? minutes : Recipe.DEFAULT_MINUTES, false, list);
    }

    private void seed(SQLiteDatabase db) {
        addSeed(db, "Scrambled Eggs", "1. Whisk eggs with salt.\n2. Melt butter in a pan.\n3. Stir eggs gently until set.",
                "egg:3:pcs", "butter:10:g", "salt:1:tsp");
        addSeed(db, "Tomato Pasta", "1. Boil pasta.\n2. Fry onion in oil, add chopped tomatoes.\n3. Simmer 10 min and toss with pasta.",
                "pasta:200:g", "tomato:3:pcs", "onion:1:pcs", "olive oil:1:tbsp");
        addSeed(db, "Cheese Omelette", "1. Beat eggs.\n2. Cook in buttered pan.\n3. Add cheese, fold and serve.",
                "egg:2:pcs", "cheese:50:g", "butter:10:g");
        addSeed(db, "Fried Rice", "1. Fry onion.\n2. Add cooked rice and scrambled egg.\n3. Season with soy sauce.",
                "rice:200:g", "egg:2:pcs", "onion:1:pcs", "soy sauce:2:tbsp");
        addSeed(db, "Pancakes", "1. Mix flour, sugar, milk and egg.\n2. Fry ladlefuls until golden both sides.",
                "flour:150:g", "milk:250:ml", "egg:1:pcs", "sugar:1:tbsp");
        addSeed(db, "Mashed Potatoes", "1. Boil peeled potatoes.\n2. Mash with butter, milk and salt.",
                "potato:4:pcs", "butter:30:g", "milk:50:ml", "salt:1:tsp");
        addSeed(db, "Garlic Bread", "1. Mix butter with crushed garlic.\n2. Spread on bread.\n3. Bake 10 min at 180C.",
                "bread:4:pcs", "butter:40:g", "garlic:2:pcs");
        addSeed(db, "Tomato Soup", "1. Fry onion and garlic.\n2. Add tomatoes and stock.\n3. Simmer 20 min and blend.",
                "tomato:6:pcs", "onion:1:pcs", "garlic:2:pcs", "vegetable stock:500:ml");
        addSeed(db, "Cheese Toastie", "1. Butter bread outsides.\n2. Fill with cheese.\n3. Toast until golden.",
                "bread:2:pcs", "cheese:40:g", "butter:10:g");
        addSeed(db, "Chicken Rice Bowl", "1. Season and pan-fry chicken.\n2. Cook rice.\n3. Serve with fried onion.",
                "chicken:300:g", "rice:200:g", "onion:1:pcs", "salt:1:tsp");
        addSeed(db, "Bean Chilli", "1. Fry onion.\n2. Add beans, tomatoes and chilli powder.\n3. Simmer 20 min.",
                "beans:400:g", "tomato:3:pcs", "onion:1:pcs", "chili powder:1:tsp");
        addSeed(db, "Banana Pancakes", "1. Mash bananas.\n2. Mix with eggs and flour.\n3. Fry small pancakes.",
                "banana:2:pcs", "egg:2:pcs", "flour:50:g");
        addSeed(db, "Potato Omelette", "1. Fry sliced potato and onion in oil.\n2. Pour in beaten eggs.\n3. Cook both sides.",
                "potato:2:pcs", "egg:4:pcs", "onion:1:pcs", "olive oil:2:tbsp");
        addSeed(db, "Mac and Cheese", "1. Boil pasta.\n2. Melt butter, add milk and cheese.\n3. Stir through pasta.",
                "pasta:250:g", "cheese:150:g", "milk:200:ml", "butter:20:g");
        addSeed(db, "Garlic Butter Chicken", "1. Melt butter with garlic.\n2. Cook chicken until golden and done.",
                "chicken:400:g", "garlic:3:pcs", "butter:30:g");
        addSeed(db, "Veggie Stir Fry", "1. Slice carrot and onion.\n2. Stir-fry, add soy sauce.\n3. Serve on rice.",
                "carrot:2:pcs", "onion:1:pcs", "soy sauce:2:tbsp", "rice:150:g");
        addSeed(db, "French Toast", "1. Whisk eggs with milk.\n2. Dip bread slices.\n3. Fry in butter until golden.",
                "bread:4:pcs", "egg:2:pcs", "milk:100:ml", "butter:10:g");
        addSeed(db, "Vegetable Soup", "1. Chop carrot, potato and onion.\n2. Simmer in stock for 25 min.\n3. Season and serve.",
                "carrot:2:pcs", "potato:2:pcs", "onion:1:pcs", "vegetable stock:500:ml");
        addSeed(db, "Cheesy Rice", "1. Cook rice.\n2. Stir in butter and grated cheese until melted.",
                "rice:200:g", "cheese:80:g", "butter:10:g");
        addSeed(db, "Garlic Fried Rice", "1. Fry crushed garlic.\n2. Add cooked rice and egg.\n3. Season with soy sauce.",
                "rice:200:g", "garlic:3:pcs", "egg:2:pcs", "soy sauce:1:tbsp");
        addSeed(db, "Eggplant Pasta", "1. Boil pasta.\n2. Fry diced eggplant and garlic, add tomatoes.\n3. Simmer 10 min and toss with pasta.",
                "pasta:200:g", "eggplant:1:pcs", "tomato:2:pcs", "garlic:2:pcs");
    }
}
