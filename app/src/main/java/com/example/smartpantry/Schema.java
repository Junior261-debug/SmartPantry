package com.example.smartpantry;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The database schema and its migrations, as plain SQL strings (no Android imports, so the SQL
 * can be exercised outside the device).
 *
 * HOW TO CHANGE THE SCHEMA LATER
 *   1. Bump {@link #VERSION}.
 *   2. Add an {@code if (from < N && to >= N)} block to {@link #upgrade(int, int)} with the
 *      ALTER/CREATE statements for version N. Never DROP user data (pantry, custom recipes).
 *   3. Fresh installs build the newest schema through {@link #create()}, which is defined as
 *      "version 1 + every migration", so a new install and an upgraded install always end up
 *      with exactly the same schema.
 *
 * History
 *   v1  pantry, recipes, recipe_ingredients
 *   v2  recipes.minutes (for the "quick meals" filter), recipes.is_custom (user-created recipes),
 *       indexes on recipe_ingredients.recipe_id and pantry.expiry
 */
public final class Schema {
    private Schema() {}

    public static final int VERSION = 3;

    /** Cooking time of each seeded recipe, in minutes. Single source of truth for seed + migration. */
    public static final Map<String, Integer> SEED_MINUTES = new LinkedHashMap<>();
    static {
        SEED_MINUTES.put("Scrambled Eggs", 10);
        SEED_MINUTES.put("Tomato Pasta", 25);
        SEED_MINUTES.put("Cheese Omelette", 10);
        SEED_MINUTES.put("Fried Rice", 20);
        SEED_MINUTES.put("Pancakes", 20);
        SEED_MINUTES.put("Mashed Potatoes", 30);
        SEED_MINUTES.put("Garlic Bread", 15);
        SEED_MINUTES.put("Tomato Soup", 35);
        SEED_MINUTES.put("Cheese Toastie", 10);
        SEED_MINUTES.put("Chicken Rice Bowl", 35);
        SEED_MINUTES.put("Bean Chilli", 30);
        SEED_MINUTES.put("Banana Pancakes", 15);
        SEED_MINUTES.put("Potato Omelette", 25);
        SEED_MINUTES.put("Mac and Cheese", 25);
        SEED_MINUTES.put("Garlic Butter Chicken", 25);
        SEED_MINUTES.put("Veggie Stir Fry", 20);
        SEED_MINUTES.put("French Toast", 15);
        SEED_MINUTES.put("Vegetable Soup", 35);
        SEED_MINUTES.put("Cheesy Rice", 20);
        SEED_MINUTES.put("Garlic Fried Rice", 15);
        SEED_MINUTES.put("Eggplant Pasta", 30);
    }

    /** The original version-1 tables. */
    static List<String> baseV1() {
        List<String> s = new ArrayList<>();
        s.add("CREATE TABLE pantry (id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "name TEXT NOT NULL, quantity REAL NOT NULL, unit TEXT NOT NULL, expiry TEXT)");
        s.add("CREATE TABLE recipes (id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "name TEXT NOT NULL, steps TEXT NOT NULL)");
        s.add("CREATE TABLE recipe_ingredients (id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "recipe_id INTEGER NOT NULL, name TEXT NOT NULL, quantity REAL NOT NULL, unit TEXT NOT NULL, "
                + "FOREIGN KEY(recipe_id) REFERENCES recipes(id))");
        return s;
    }

    /** Statements for a brand-new database (the newest schema, no data). */
    public static List<String> create() {
        List<String> s = baseV1();
        s.addAll(upgrade(1, VERSION));
        return s;
    }

    /** Statements that move an existing database from version {@code from} to {@code to}. */
    public static List<String> upgrade(int from, int to) {
        List<String> s = new ArrayList<>();
        if (from < 2 && to >= 2) {
            s.add("ALTER TABLE recipes ADD COLUMN minutes INTEGER NOT NULL DEFAULT " + Recipe.DEFAULT_MINUTES);
            s.add("ALTER TABLE recipes ADD COLUMN is_custom INTEGER NOT NULL DEFAULT 0");
            s.add("CREATE INDEX idx_ingredients_recipe ON recipe_ingredients(recipe_id)");
            s.add("CREATE INDEX idx_pantry_expiry ON pantry(expiry)");
            for (Map.Entry<String, Integer> e : SEED_MINUTES.entrySet()) {
                s.add("UPDATE recipes SET minutes = " + e.getValue()
                        + " WHERE name = '" + e.getKey().replace("'", "''") + "' AND is_custom = 0");
            }
        }
        // v3: repair/backfill missing bundled recipes from existing installs.
        // Data insertion is performed by DbHelper.onUpgrade() after migrations.
        return s;
    }
}
