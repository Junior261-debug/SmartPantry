package com.example.smartpantry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RecipeMatcherTest {

    private static final long TODAY = DateUtil.epochDay(2026, 10, 19);

    // ---- helpers -----------------------------------------------------------------------------

    private static PantryItem item(String name, double qty, String unit) {
        return new PantryItem(name, qty, unit, null);
    }

    private static PantryItem item(long id, String name, double qty, String unit, String expiry) {
        return new PantryItem(id, name, qty, unit, expiry);
    }

    /** 5-ingredient recipe: pasta, tomato, onion, garlic, olive oil. */
    private static Recipe build5() {
        Recipe r = new Recipe(1, "Five", "");
        r.add("pasta", 200, "g").add("tomato", 3, "pcs").add("onion", 1, "pcs")
                .add("garlic", 2, "pcs").add("olive oil", 1, "tbsp");
        return r;
    }

    private static List<Recipe> list(Recipe... r) { return new ArrayList<>(Arrays.asList(r)); }

    private static boolean strictlySuggested(Recipe r, List<PantryItem> pantry) {
        return RecipeMatcher.suggest(list(r), pantry).contains(r);
    }

    // ---- strict rule -------------------------------------------------------------------------

    @Test
    public void allFiveIngredientsPresent_isSuggested() {
        List<PantryItem> pantry = Arrays.asList(
                item("pasta", 500, "g"), item("tomato", 5, "pcs"), item("onion", 2, "pcs"),
                item("garlic", 4, "pcs"), item("olive oil", 5, "tbsp"));
        assertTrue(strictlySuggested(build5(), pantry));
    }

    @Test
    public void fourOfFiveIngredients_isNotSuggested() {
        List<PantryItem> pantry = Arrays.asList(           // no olive oil
                item("pasta", 500, "g"), item("tomato", 5, "pcs"), item("onion", 2, "pcs"),
                item("garlic", 4, "pcs"));
        assertFalse(strictlySuggested(build5(), pantry));
    }

    @Test
    public void everyIngredientPresentButOneTooSmall_isNotSuggested() {
        List<PantryItem> pantry = Arrays.asList(
                item("pasta", 150, "g"),                   // needs 200 g
                item("tomato", 5, "pcs"), item("onion", 2, "pcs"),
                item("garlic", 4, "pcs"), item("olive oil", 5, "tbsp"));
        assertFalse(strictlySuggested(build5(), pantry));
    }

    @Test
    public void recipeWithNoIngredients_isNeverSuggested() {
        assertFalse(strictlySuggested(new Recipe(9, "Empty", ""), new ArrayList<PantryItem>()));
    }

    @Test
    public void exactQuantity_isEnough() {
        Recipe r = new Recipe(1, "R", "").add("flour", 150, "g");
        assertTrue(strictlySuggested(r, Arrays.asList(item("flour", 150, "g"))));
    }

    // ---- names -------------------------------------------------------------------------------

    @Test
    public void pluralAndCaseInsensitiveNamesMatch() {
        Recipe r = new Recipe(1, "R", "")
                .add("tomato", 3, "pcs").add("egg", 2, "pcs").add("berry", 100, "g").add("potato", 1, "pcs");
        List<PantryItem> pantry = Arrays.asList(
                item("  Tomatoes ", 3, "pcs"), item("EGGS", 6, "pcs"),
                item("Berries", 200, "g"), item("potatoes", 2, "pcs"));
        assertTrue(strictlySuggested(r, pantry));
    }

    @Test
    public void pluralOnTheRecipeSideMatchesSingularInPantry() {
        Recipe r = new Recipe(1, "R", "").add("onions", 2, "pcs").add("cookies", 2, "pcs");
        assertTrue(strictlySuggested(r, Arrays.asList(item("onion", 2, "pcs"), item("cookie", 4, "pcs"))));
    }

    @Test
    public void normaliseHandlesCommonPlurals() {
        assertEquals("tomato", RecipeMatcher.normalise("Tomatoes"));
        assertEquals("berry", RecipeMatcher.normalise("berries"));
        assertEquals("egg", RecipeMatcher.normalise(" EGGS "));
        assertEquals("peach", RecipeMatcher.normalise("peaches"));
        assertEquals("olive oil", RecipeMatcher.normalise("olive   oil"));
        assertEquals("hummus", RecipeMatcher.normalise("hummus"));      // not "hummu"
        assertEquals("glass", RecipeMatcher.normalise("glass"));        // "ss" is left alone
        assertEquals("", RecipeMatcher.normalise(null));
    }

    @Test
    public void differentIngredientsDoNotMatch() {
        Recipe r = new Recipe(1, "R", "").add("tomato", 1, "pcs");
        assertFalse(strictlySuggested(r, Arrays.asList(item("potato", 5, "pcs"))));
    }

    // ---- synonyms ----------------------------------------------------------------------------

    @Test
    public void aubergineSatisfiesEggplant() {
        Recipe r = new Recipe(1, "R", "").add("eggplant", 1, "pcs");
        assertTrue(strictlySuggested(r, Arrays.asList(item("Aubergine", 2, "pcs"))));
        assertTrue(strictlySuggested(r, Arrays.asList(item("aubergines", 2, "pcs"))));
    }

    @Test
    public void corianderSatisfiesCilantroAndViceVersa() {
        Recipe cilantro = new Recipe(1, "R", "").add("cilantro", 10, "g");
        Recipe coriander = new Recipe(2, "R", "").add("coriander", 10, "g");
        assertTrue(strictlySuggested(cilantro, Arrays.asList(item("coriander", 20, "g"))));
        assertTrue(strictlySuggested(coriander, Arrays.asList(item("cilantro", 20, "g"))));
    }

    @Test
    public void otherSynonymsAndPhrases() {
        assertEquals(RecipeMatcher.normalise("zucchini"), RecipeMatcher.normalise("Courgettes"));
        assertEquals(RecipeMatcher.normalise("shrimp"), RecipeMatcher.normalise("prawns"));
        assertEquals(RecipeMatcher.normalise("bell pepper"), RecipeMatcher.normalise("capsicum"));
        assertEquals(RecipeMatcher.normalise("green onion"), RecipeMatcher.normalise("Spring Onions"));
        assertEquals(RecipeMatcher.normalise("chili"), RecipeMatcher.normalise("chillies"));
        assertEquals(RecipeMatcher.normalise("chili powder"), RecipeMatcher.normalise("chilli powder"));
        assertEquals(RecipeMatcher.normalise("ground beef"), RecipeMatcher.normalise("minced beef"));
    }

    // ---- units -------------------------------------------------------------------------------

    @Test
    public void oneKilogramSatisfiesFiveHundredGrams() {
        Recipe r = new Recipe(1, "R", "").add("flour", 500, "g");
        assertTrue(strictlySuggested(r, Arrays.asList(item("flour", 1, "kg"))));
    }

    @Test
    public void fiveHundredGramsDoesNotSatisfyOneKilogram() {
        Recipe r = new Recipe(1, "R", "").add("flour", 1, "kg");
        assertFalse(strictlySuggested(r, Arrays.asList(item("flour", 500, "g"))));
    }

    @Test
    public void litresMillilitresAndKitchenSpoons() {
        Recipe milk = new Recipe(1, "R", "").add("milk", 250, "ml");
        assertTrue(strictlySuggested(milk, Arrays.asList(item("milk", 1, "l"))));
        Recipe oil = new Recipe(2, "R", "").add("olive oil", 2, "tbsp");
        assertTrue(strictlySuggested(oil, Arrays.asList(item("olive oil", 100, "ml"))));
        assertFalse(strictlySuggested(oil, Arrays.asList(item("olive oil", 20, "ml"))));  // 2 tbsp = 30 ml
    }

    @Test
    public void unitAliasesAreUnderstood() {
        Recipe r = new Recipe(1, "R", "").add("rice", 200, "g");
        assertTrue(strictlySuggested(r, Arrays.asList(item("rice", 1, "Kilograms"))));
    }

    @Test
    public void incompatibleUnitFamiliesNeverMatch() {
        Recipe r = new Recipe(1, "R", "").add("milk", 200, "ml");
        assertFalse(strictlySuggested(r, Arrays.asList(item("milk", 5, "kg"))));
        Recipe eggs = new Recipe(2, "R", "").add("egg", 2, "pcs");
        assertFalse(strictlySuggested(eggs, Arrays.asList(item("egg", 500, "g"))));
    }

    @Test
    public void duplicatePantryRowsAreSummed() {
        Recipe r = new Recipe(1, "R", "").add("flour", 500, "g");
        assertTrue(strictlySuggested(r, Arrays.asList(item("flour", 300, "g"), item("Flour", 0.2, "kg"))));
    }

    // ---- almost there ------------------------------------------------------------------------

    @Test
    public void almostThere_listsRecipesMissingExactlyOneIngredient() {
        Recipe five = build5();
        List<PantryItem> pantry = Arrays.asList(           // no olive oil -> exactly one missing
                item("pasta", 500, "g"), item("tomato", 5, "pcs"), item("onion", 2, "pcs"),
                item("garlic", 4, "pcs"));
        List<MatchResult> almost = RecipeMatcher.almostThere(list(five), pantry, TODAY, 3);
        List<MatchResult> ready = RecipeMatcher.ready(list(five), pantry, TODAY, 3);

        assertEquals(1, almost.size());
        assertTrue(ready.isEmpty());                       // never in the strict list as well
        MatchResult.Gap gap = almost.get(0).gaps.get(0);
        assertEquals("olive oil", gap.ingredient.name);
        assertTrue(gap.absent);
    }

    @Test
    public void almostThere_excludesRecipesMissingTwoOrMore() {
        List<PantryItem> pantry = Arrays.asList(item("pasta", 500, "g"), item("tomato", 5, "pcs"),
                item("onion", 2, "pcs"));                  // garlic and olive oil both missing
        assertTrue(RecipeMatcher.almostThere(list(build5()), pantry, TODAY, 3).isEmpty());
    }

    @Test
    public void almostThere_countsAnInsufficientQuantityAsTheOneGap() {
        Recipe r = new Recipe(1, "R", "").add("flour", 500, "g").add("egg", 2, "pcs");
        List<PantryItem> pantry = Arrays.asList(item("flour", 350, "g"), item("egg", 6, "pcs"));
        List<MatchResult> almost = RecipeMatcher.almostThere(list(r), pantry, TODAY, 3);
        assertEquals(1, almost.size());
        MatchResult.Gap gap = almost.get(0).gaps.get(0);
        assertFalse(gap.absent);
        assertEquals(150, gap.shortBy, 1e-6);
        assertEquals("need 150 g more flour", gap.describe());
    }

    @Test
    public void almostThere_hasNothingWhenRecipeIsReady() {
        Recipe r = new Recipe(1, "R", "").add("egg", 2, "pcs");
        assertTrue(RecipeMatcher.almostThere(list(r), Arrays.asList(item("egg", 2, "pcs")), TODAY, 3).isEmpty());
        assertEquals(1, RecipeMatcher.ready(list(r), Arrays.asList(item("egg", 2, "pcs")), TODAY, 3).size());
    }

    // ---- expiry-first ranking ----------------------------------------------------------------

    @Test
    public void readyRecipesAreRankedByHowManyExpiringIngredientsTheyUse() {
        Recipe none = new Recipe(1, "A none", "").add("rice", 100, "g").add("salt", 1, "tsp");
        Recipe one = new Recipe(2, "B one", "").add("milk", 100, "ml").add("salt", 1, "tsp");
        Recipe two = new Recipe(3, "C two", "").add("milk", 100, "ml").add("egg", 1, "pcs");
        List<PantryItem> pantry = Arrays.asList(
                item(1, "rice", 500, "g", "2027-01-01"),
                item(2, "salt", 50, "tsp", null),
                item(3, "milk", 1, "l", "2026-10-20"),     // expires tomorrow
                item(4, "egg", 6, "pcs", "2026-10-21"));   // expires in 2 days

        List<MatchResult> ready = RecipeMatcher.ready(list(none, one, two), pantry, TODAY, 3);

        assertEquals(3, ready.size());
        assertEquals("C two", ready.get(0).recipe.name);   // uses milk + egg
        assertEquals(2, ready.get(0).expiringUsed);
        assertEquals("B one", ready.get(1).recipe.name);   // uses milk
        assertEquals(1, ready.get(1).expiringUsed);
        assertEquals("A none", ready.get(2).recipe.name);
        assertEquals(0, ready.get(2).expiringUsed);
    }

    @Test
    public void itemsOutsideTheSoonWindowOrWithoutDateDoNotCount() {
        Recipe r = new Recipe(1, "R", "").add("milk", 100, "ml").add("egg", 1, "pcs");
        List<PantryItem> pantry = Arrays.asList(
                item(1, "milk", 1, "l", "2026-10-30"),     // 11 days away
                item(2, "egg", 6, "pcs", null));
        assertEquals(0, RecipeMatcher.ready(list(r), pantry, TODAY, 3).get(0).expiringUsed);
    }

    @Test
    public void alreadyExpiredAndTodayCountAsExpiring() {
        Recipe r = new Recipe(1, "R", "").add("milk", 100, "ml").add("egg", 1, "pcs");
        List<PantryItem> pantry = Arrays.asList(
                item(1, "milk", 1, "l", "2026-10-19"),     // today
                item(2, "egg", 6, "pcs", "2026-10-15"));   // 4 days ago
        MatchResult m = RecipeMatcher.ready(list(r), pantry, TODAY, 3).get(0);
        assertEquals(2, m.expiringUsed);
        assertEquals(-4, m.soonestExpiryDays);
    }

    @Test
    public void tieOnCountIsBrokenByWhicheverExpiresFirst() {
        Recipe milkRecipe = new Recipe(1, "Milk dish", "").add("milk", 100, "ml");
        Recipe eggRecipe = new Recipe(2, "Egg dish", "").add("egg", 1, "pcs");
        List<PantryItem> pantry = Arrays.asList(
                item(1, "milk", 1, "l", "2026-10-22"),     // in 3 days
                item(2, "egg", 6, "pcs", "2026-10-20"));   // tomorrow
        List<MatchResult> ready = RecipeMatcher.ready(list(milkRecipe, eggRecipe), pantry, TODAY, 3);
        assertEquals("Egg dish", ready.get(0).recipe.name);
        assertEquals("Milk dish", ready.get(1).recipe.name);
    }

    @Test
    public void expiryMatchingUsesSynonymsAndUnitFamilies() {
        Recipe r = new Recipe(1, "R", "").add("eggplant", 1, "pcs");
        List<PantryItem> pantry = Arrays.asList(item(1, "Aubergines", 2, "pcs", "2026-10-20"));
        assertEquals(1, RecipeMatcher.ready(list(r), pantry, TODAY, 3).get(0).expiringUsed);
    }

    @Test
    public void almostThereListIsAlsoRankedExpiryFirst() {
        Recipe a = new Recipe(1, "A", "").add("milk", 100, "ml").add("saffron", 1, "g");
        Recipe b = new Recipe(2, "B", "").add("egg", 1, "pcs").add("saffron", 1, "g");
        List<PantryItem> pantry = Arrays.asList(
                item(1, "milk", 1, "l", null),
                item(2, "egg", 6, "pcs", "2026-10-20"));
        List<MatchResult> almost = RecipeMatcher.almostThere(list(a, b), pantry, TODAY, 3);
        assertEquals("B", almost.get(0).recipe.name);
        assertEquals("A", almost.get(1).recipe.name);
    }
}
