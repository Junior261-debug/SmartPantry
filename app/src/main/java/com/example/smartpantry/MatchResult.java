package com.example.smartpantry;

import java.util.ArrayList;
import java.util.List;

/** How one recipe measures up against the pantry. */
public class MatchResult {

    /** One recipe ingredient the pantry cannot fully cover. */
    public static class Gap {
        public final RecipeIngredient ingredient;
        /** True if the pantry has none of it at all (otherwise it has some, but not enough). */
        public final boolean absent;
        /** How much more is needed, in the recipe's own unit. Equals the full quantity if absent. */
        public final double shortBy;

        public Gap(RecipeIngredient ingredient, boolean absent, double shortBy) {
            this.ingredient = ingredient;
            this.absent = absent;
            this.shortBy = shortBy;
        }

        /** "eggplant" when missing entirely, "need 150 g more flour" when only short. */
        public String describe() {
            if (absent) return ingredient.name;
            return "need " + Units.describe(shortBy, ingredient.unit) + " more " + ingredient.name;
        }
    }

    public final Recipe recipe;
    /** Ingredients that are missing or short. Empty = the recipe can be cooked right now. */
    public final List<Gap> gaps = new ArrayList<>();
    /** How many of the recipe's ingredients are covered by pantry items that expire soon. */
    public int expiringUsed;
    /** Days until the earliest of those items expires (negative = already expired). */
    public long soonestExpiryDays = DateUtil.NO_EXPIRY;

    public MatchResult(Recipe recipe) {
        this.recipe = recipe;
    }

    public boolean isReady() { return gaps.isEmpty(); }

    public boolean isAlmostThere() { return gaps.size() == 1; }

    public boolean usesExpiring() { return expiringUsed > 0; }
}
