package com.example.smartpantry;

/** One row of the user's pantry. {@code expiry} is "yyyy-MM-dd" or null/empty when unknown. */
public class PantryItem {
    public long id;
    public String name;
    public double quantity;
    public String unit;
    public String expiry;

    public PantryItem(long id, String name, double quantity, String unit, String expiry) {
        this.id = id;
        this.name = name;
        this.quantity = quantity;
        this.unit = unit;
        this.expiry = expiry;
    }

    /** Convenience for new items that have no database id yet. */
    public PantryItem(String name, double quantity, String unit, String expiry) {
        this(0, name, quantity, unit, expiry);
    }
}
