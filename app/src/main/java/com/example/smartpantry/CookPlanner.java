package com.example.smartpantry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Works out what "Cook this" does to the pantry, without touching a database, so the rules are
 * unit-testable. Quantities are deducted in base units (so cooking with 200 g from a 1 kg bag
 * leaves 0.8 kg), taking from the soonest-expiring matching items first. Items that reach zero
 * are flagged for deletion.
 */
public final class CookPlanner {
    private CookPlanner() {}

    private static final double EPS = 1e-6;

    /** A single pantry-row change: set a new quantity, or delete the row. */
    public static class Change {
        public final long itemId;
        public final double newQuantity;
        public final boolean delete;

        Change(long itemId, double newQuantity, boolean delete) {
            this.itemId = itemId;
            this.newQuantity = newQuantity;
            this.delete = delete;
        }
    }

    public static class Plan {
        /** False if the pantry cannot cover the recipe; then {@code changes} is empty. */
        public final boolean possible;
        public final List<Change> changes;
        /** Names of ingredients that could not be covered (only when not possible). */
        public final List<String> missing;

        Plan(boolean possible, List<Change> changes, List<String> missing) {
            this.possible = possible;
            this.changes = changes;
            this.missing = missing;
        }
    }

    public static Plan plan(Recipe recipe, List<PantryItem> pantry) {
        Map<Long, Double> remaining = new HashMap<>();   // item id -> remaining quantity, base units
        Map<Long, PantryItem> touched = new HashMap<>();
        List<String> missing = new ArrayList<>();

        for (RecipeIngredient ri : recipe.ingredients) {
            String key = RecipeMatcher.key(ri.name, ri.unit);
            double need = Units.toBase(ri.quantity, ri.unit);

            List<PantryItem> candidates = new ArrayList<>();
            for (PantryItem p : pantry) if (RecipeMatcher.key(p.name, p.unit).equals(key)) candidates.add(p);
            // soonest expiry first; items without a date last
            Collections.sort(candidates, (a, b) -> {
                int c = Long.compare(DateUtil.parseEpochDay(a.expiry), DateUtil.parseEpochDay(b.expiry));
                return c != 0 ? c : Long.compare(a.id, b.id);
            });

            for (PantryItem p : candidates) {
                if (need <= EPS) break;
                Double left = remaining.get(p.id);
                double have = left != null ? left : Units.toBase(p.quantity, p.unit);
                double take = Math.min(have, need);
                if (take <= 0) continue;
                remaining.put(p.id, have - take);
                touched.put(p.id, p);
                need -= take;
            }
            if (need > EPS) missing.add(ri.name);
        }

        if (!missing.isEmpty()) return new Plan(false, new ArrayList<>(), missing);

        List<Change> changes = new ArrayList<>();
        for (Map.Entry<Long, Double> e : remaining.entrySet()) {
            PantryItem p = touched.get(e.getKey());
            double left = e.getValue();
            if (left < EPS) {
                changes.add(new Change(p.id, 0, true));
            } else {
                double q = Math.round(Units.fromBase(left, p.unit) * 10000.0) / 10000.0;
                changes.add(new Change(p.id, q, false));
            }
        }
        Collections.sort(changes, (a, b) -> Long.compare(a.itemId, b.itemId));
        return new Plan(true, changes, missing);
    }
}
