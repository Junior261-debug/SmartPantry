package com.example.smartpantry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * THE STRICT-MATCHING RULE (brief section 2.3):
 * a recipe is "ready" only if EVERY ingredient is in the pantry in at least the required
 * quantity. 4 of 5 ingredients = not ready.
 *
 * On top of that, recipes that are short by exactly ONE ingredient are reported separately as
 * "almost there", and both lists are ranked so recipes that use up soon-to-expire food come first.
 *
 * Pure Java (no Android imports) so it is covered by plain JUnit tests.
 */
public class RecipeMatcher {

    /** Items expiring within this many days (or already expired) count as "soon to expire". */
    public static final int SOON_DAYS = 3;

    // ------------------------------------------------------------------ names

    /**
     * Normalises names so "Tomatoes", " tomato " and "TOMATO" are equal, and so regional names
     * collapse onto one canonical name ("aubergine" -> "eggplant", "coriander" -> "cilantro").
     */
    public static String normalise(String name) {
        if (name == null) return "";
        String n = name.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9 ]", " ").replaceAll("\\s+", " ").trim();
        if (n.isEmpty()) return n;
        StringBuilder sb = new StringBuilder();
        for (String word : n.split(" ")) {
            if (sb.length() > 0) sb.append(' ');
            sb.append(Synonyms.mapWord(singular(word)));
        }
        return Synonyms.mapPhrase(sb.toString());
    }

    /** Naive English singulariser: berries -> berry, tomatoes -> tomato, eggs -> egg. */
    static String singular(String w) {
        int len = w.length();
        if (w.endsWith("ies") && len > 4) return w.substring(0, len - 3) + "y";          // berries -> berry
        if (w.endsWith("ie") && len > 4) return w.substring(0, len - 2) + "y";           // cookie -> cooky (so cookies == cookie)
        if (w.endsWith("oes") && len > 3) return w.substring(0, len - 2);                // tomatoes -> tomato
        if ((w.endsWith("ches") || w.endsWith("shes") || w.endsWith("xes")
                || w.endsWith("sses")) && len > 4) return w.substring(0, len - 2);        // peaches -> peach
        if (w.endsWith("s") && !w.endsWith("ss") && !w.endsWith("us") && !w.endsWith("is")
                && len > 3) return w.substring(0, len - 1);                              // eggs -> egg
        return w;
    }

    static String key(String name, String unit) {
        return normalise(name) + "|" + Units.family(unit);
    }

    // ------------------------------------------------------------------ stock

    /** Totals the pantry per (ingredient, unit family), in base units. Duplicate rows are summed. */
    public static Map<String, Double> buildStock(List<PantryItem> pantry) {
        Map<String, Double> stock = new HashMap<>();
        for (PantryItem p : pantry) {
            String k = key(p.name, p.unit);
            double base = Units.toBase(p.quantity, p.unit);
            Double current = stock.get(k);
            stock.put(k, (current == null ? 0 : current) + base);
        }
        return stock;
    }

    /** Per ingredient key: days until the soonest-expiring item (only items within {@code soonDays}). */
    private static Map<String, Long> buildExpiring(List<PantryItem> pantry, long today, int soonDays) {
        Map<String, Long> soon = new HashMap<>();
        for (PantryItem p : pantry) {
            Long days = DateUtil.daysUntil(p.expiry, today);
            if (days == null || days > soonDays) continue;
            String k = key(p.name, p.unit);
            Long current = soon.get(k);
            if (current == null || days < current) soon.put(k, days);
        }
        return soon;
    }

    // ------------------------------------------------------------------ strict rule

    public static boolean canMake(Recipe recipe, Map<String, Double> stock) {
        if (recipe.ingredients.isEmpty()) return false;
        for (RecipeIngredient ri : recipe.ingredients) {
            Double have = stock.get(key(ri.name, ri.unit));
            if (have == null) return false;                                // ingredient missing
            if (have + 1e-9 < Units.toBase(ri.quantity, ri.unit)) return false; // not enough
        }
        return true;
    }

    /** Strict list, unranked. Kept for callers that only need the yes/no answer. */
    public static List<Recipe> suggest(List<Recipe> all, List<PantryItem> pantry) {
        Map<String, Double> stock = buildStock(pantry);
        List<Recipe> result = new ArrayList<>();
        for (Recipe r : all) if (canMake(r, stock)) result.add(r);
        return result;
    }

    // ------------------------------------------------------------------ ranked results

    /** Evaluates one recipe: which ingredients are short, and how many use soon-to-expire food. */
    static MatchResult evaluate(Recipe r, Map<String, Double> stock, Map<String, Long> expiring) {
        MatchResult m = new MatchResult(r);
        for (RecipeIngredient ri : r.ingredients) {
            String k = key(ri.name, ri.unit);
            double need = Units.toBase(ri.quantity, ri.unit);
            Double have = stock.get(k);
            if (have == null) {
                m.gaps.add(new MatchResult.Gap(ri, true, ri.quantity));
            } else if (have + 1e-9 < need) {
                m.gaps.add(new MatchResult.Gap(ri, false, Units.fromBase(need - have, ri.unit)));
            } else {
                Long days = expiring.get(k);
                if (days != null) {
                    m.expiringUsed++;
                    if (days < m.soonestExpiryDays) m.soonestExpiryDays = days;
                }
            }
        }
        return m;
    }

    /**
     * Most soon-to-expire ingredients used first; ties go to whichever expires earliest, then to
     * the quicker recipe, then alphabetically so the order is stable.
     */
    static final Comparator<MatchResult> EXPIRY_FIRST = (a, b) -> {
        if (a.expiringUsed != b.expiringUsed) return Integer.compare(b.expiringUsed, a.expiringUsed);
        if (a.soonestExpiryDays != b.soonestExpiryDays) return Long.compare(a.soonestExpiryDays, b.soonestExpiryDays);
        if (a.recipe.minutes != b.recipe.minutes) return Integer.compare(a.recipe.minutes, b.recipe.minutes);
        return a.recipe.name.compareToIgnoreCase(b.recipe.name);
    };

    /** Evaluates a single recipe against the pantry (used by the recipe detail screen). */
    public static MatchResult analyse(Recipe r, List<PantryItem> pantry, long today, int soonDays) {
        return evaluate(r, buildStock(pantry), buildExpiring(pantry, today, soonDays));
    }

    /** Recipes that can be cooked right now, expiry-first. */
    public static List<MatchResult> ready(List<Recipe> all, List<PantryItem> pantry, long today, int soonDays) {
        return collect(all, pantry, today, soonDays, 0);
    }

    /** Recipes short by exactly one ingredient (missing, or not enough of it), expiry-first. */
    public static List<MatchResult> almostThere(List<Recipe> all, List<PantryItem> pantry, long today, int soonDays) {
        return collect(all, pantry, today, soonDays, 1);
    }

    private static List<MatchResult> collect(List<Recipe> all, List<PantryItem> pantry, long today,
                                             int soonDays, int wantedGaps) {
        Map<String, Double> stock = buildStock(pantry);
        Map<String, Long> expiring = buildExpiring(pantry, today, soonDays);
        List<MatchResult> out = new ArrayList<>();
        for (Recipe r : all) {
            if (r.ingredients.isEmpty()) continue;
            MatchResult m = evaluate(r, stock, expiring);
            if (m.gaps.size() == wantedGaps) out.add(m);
        }
        Collections.sort(out, EXPIRY_FIRST);
        return out;
    }
}
