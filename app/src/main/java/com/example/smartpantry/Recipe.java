package com.example.smartpantry;

import java.util.ArrayList;
import java.util.List;

public class Recipe {
    /** Recipes taking this many minutes or fewer count as a "quick meal". */
    public static final int QUICK_MINUTES = 20;
    public static final int DEFAULT_MINUTES = 30;

    public long id;
    public String name;
    public String steps;
    /** Total preparation + cooking time. */
    public int minutes = DEFAULT_MINUTES;
    /** True for recipes the user created; seeded recipes are false and cannot be deleted. */
    public boolean custom;
    public final List<RecipeIngredient> ingredients = new ArrayList<>();

    public Recipe(long id, String name, String steps) {
        this.id = id;
        this.name = name;
        this.steps = steps;
    }

    public boolean isQuick() {
        return minutes <= QUICK_MINUTES;
    }

    public Recipe add(String ingredient, double quantity, String unit) {
        ingredients.add(new RecipeIngredient(ingredient, quantity, unit));
        return this;
    }
}
