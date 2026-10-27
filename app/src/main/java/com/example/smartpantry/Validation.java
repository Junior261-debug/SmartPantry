package com.example.smartpantry;


/** Input validation shared by the Add/Edit ingredient and Add recipe forms. Pure Java. */
public final class Validation {
    private Validation() {}

    public static final int MAX_NAME = 60;

    /** Trimmed, single-spaced name; null if blank, too long, or containing no letters. */
    public static String name(String raw) {
        if (raw == null) return null;
        String n = raw.trim().replaceAll("\\s+", " ");
        if (n.isEmpty() || n.length() > MAX_NAME) return null;
        if (!n.matches(".*\\p{L}.*")) return null;      // "123" or "---" is not an ingredient
        return n;
    }

    /** A quantity above 0 (comma decimals accepted); null if not a valid positive number. */
    public static Double quantity(String raw) {
        if (raw == null) return null;
        try {
            double v = Double.parseDouble(raw.trim().replace(',', '.'));
            if (Double.isNaN(v) || Double.isInfinite(v) || v <= 0 || v > 1_000_000) return null;
            return v;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Cooking time in whole minutes, 1..1440; null otherwise. */
    public static Integer minutes(String raw) {
        if (raw == null) return null;
        try {
            int v = Integer.parseInt(raw.trim());
            return v >= 1 && v <= 1440 ? v : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** True only for one of the units offered in the dropdown (case-insensitive). */
    public static boolean unit(String raw) {
        return raw != null && Units.CHOICES.contains(Units.canonical(raw));
    }
}
