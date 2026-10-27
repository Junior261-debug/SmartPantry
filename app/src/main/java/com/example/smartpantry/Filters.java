package com.example.smartpantry;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Search box on the pantry list and filter chips on the suggestions list. */
public final class Filters {
    private Filters() {}

    /**
     * Pantry search. Matches on the typed text, and also on the normalised name, so searching
     * "tomato" finds "Tomatoes" and searching "aubergine" finds "Eggplant".
     */
    public static List<PantryItem> pantry(List<PantryItem> items, String query) {
        if (query == null || query.trim().isEmpty()) return items;
        String raw = query.trim().toLowerCase(Locale.ROOT);
        String norm = RecipeMatcher.normalise(raw);
        List<PantryItem> out = new ArrayList<>();
        for (PantryItem p : items) {
            String name = p.name == null ? "" : p.name.toLowerCase(Locale.ROOT);
            if (name.contains(raw) || (!norm.isEmpty() && RecipeMatcher.normalise(p.name).contains(norm))) out.add(p);
        }
        return out;
    }

    /** Suggestion filters; every ticked filter must hold. */
    public static List<MatchResult> suggestions(List<MatchResult> in, boolean quickOnly,
                                                boolean usesExpiringOnly, boolean myRecipesOnly) {
        List<MatchResult> out = new ArrayList<>();
        for (MatchResult m : in) {
            if (quickOnly && !m.recipe.isQuick()) continue;
            if (usesExpiringOnly && !m.usesExpiring()) continue;
            if (myRecipesOnly && !m.recipe.custom) continue;
            out.add(m);
        }
        return out;
    }
}
