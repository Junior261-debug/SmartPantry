package com.example.smartpantry;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Unit handling. Every unit belongs to a "family" (mass, volume, count) and has a factor to the
 * family's base unit (g, ml, pcs). Quantities are only comparable within one family, so
 * 1 kg satisfies 500 g, but 500 ml never satisfies 500 g.
 * Pure Java (no Android imports) so it can be unit-tested on the JVM.
 */
public final class Units {
    private Units() {}

    /** Units offered in the UI, in display order. */
    public static final List<String> CHOICES =
            Arrays.asList("g", "kg", "ml", "l", "tsp", "tbsp", "cup", "pcs", "oz", "lb");

    private static final Map<String, String> ALIASES = new HashMap<>();
    private static final Map<String, String> FAMILY = new HashMap<>();
    private static final Map<String, Double> FACTOR = new HashMap<>();

    private static void def(String unit, String family, double factor, String... aliases) {
        FAMILY.put(unit, family);
        FACTOR.put(unit, factor);
        ALIASES.put(unit, unit);
        for (String a : aliases) ALIASES.put(a, unit);
    }

    static {
        def("g", "mass", 1, "gram", "grams", "gm");
        def("kg", "mass", 1000, "kgs", "kilo", "kilos", "kilogram", "kilograms");
        def("oz", "mass", 28.3495, "ounce", "ounces");
        def("lb", "mass", 453.592, "lbs", "pound", "pounds");
        def("ml", "volume", 1, "milliliter", "milliliters", "millilitre", "millilitres");
        def("l", "volume", 1000, "liter", "liters", "litre", "litres");
        def("tsp", "volume", 5, "teaspoon", "teaspoons");
        def("tbsp", "volume", 15, "tablespoon", "tablespoons");
        def("cup", "volume", 240, "cups");
        def("pcs", "count", 1, "pc", "piece", "pieces", "each", "unit", "units", "whole");
    }

    /** Canonical spelling of a unit ("Litres" -> "l"); unknown units are lower-cased and kept. */
    public static String canonical(String unit) {
        if (unit == null) return "pcs";
        String u = unit.trim().toLowerCase(Locale.ROOT);
        String c = ALIASES.get(u);
        return c != null ? c : u;
    }

    /** "mass", "volume", "count", or "other:&lt;unit&gt;" for units we do not know. */
    public static String family(String unit) {
        String c = canonical(unit);
        String f = FAMILY.get(c);
        return f != null ? f : "other:" + c;
    }

    public static double factor(String unit) {
        Double f = FACTOR.get(canonical(unit));
        return f != null ? f : 1.0;
    }

    /** Converts a quantity to its family's base unit (g, ml or pcs). */
    public static double toBase(double quantity, String unit) {
        return quantity * factor(unit);
    }

    /** Converts a base-unit amount back into the given unit. */
    public static double fromBase(double base, String unit) {
        return base / factor(unit);
    }

    /** 2.0 -> "2", 0.25 -> "0.25", 1.3333 -> "1.33". */
    public static String format(double q) {
        if (Math.abs(q - Math.rint(q)) < 1e-9) return String.valueOf((long) Math.rint(q));
        String s = String.format(Locale.US, "%.2f", q);
        if (s.contains(".")) s = s.replaceAll("0+$", "").replaceAll("\\.$", "");
        return s;
    }

    public static String describe(double q, String unit) {
        return format(q) + " " + unit;
    }
}
