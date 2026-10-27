package com.example.smartpantry;

/** One line of a recipe, e.g. "tomato", 3, "pcs". */
public class RecipeIngredient {
    public final String name;
    public final double quantity;
    public final String unit;

    public RecipeIngredient(String name, double quantity, String unit) {
        this.name = name;
        this.quantity = quantity;
        this.unit = unit;
    }
}
