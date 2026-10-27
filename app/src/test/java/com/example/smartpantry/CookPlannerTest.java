package com.example.smartpantry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class CookPlannerTest {

    private static PantryItem item(long id, String name, double qty, String unit, String expiry) {
        return new PantryItem(id, name, qty, unit, expiry);
    }

    private static CookPlanner.Change change(CookPlanner.Plan p, long id) {
        for (CookPlanner.Change c : p.changes) if (c.itemId == id) return c;
        return null;
    }

    @Test
    public void deductsQuantityFromThePantry() {
        Recipe r = new Recipe(1, "R", "").add("egg", 2, "pcs");
        CookPlanner.Plan plan = CookPlanner.plan(r, Arrays.asList(item(1, "egg", 6, "pcs", null)));
        assertTrue(plan.possible);
        assertEquals(1, plan.changes.size());
        assertFalse(change(plan, 1).delete);
        assertEquals(4, change(plan, 1).newQuantity, 1e-9);
    }

    @Test
    public void itemsThatReachZeroAreRemoved() {
        Recipe r = new Recipe(1, "R", "").add("egg", 3, "pcs").add("butter", 10, "g");
        List<PantryItem> pantry = Arrays.asList(
                item(1, "Eggs", 3, "pcs", null), item(2, "butter", 250, "g", null));
        CookPlanner.Plan plan = CookPlanner.plan(r, pantry);
        assertTrue(plan.possible);
        assertTrue(change(plan, 1).delete);                // 3 - 3 = 0 -> removed
        assertEquals(240, change(plan, 2).newQuantity, 1e-9);
    }

    @Test
    public void deductionIsConvertedBackIntoThePantryItemsOwnUnit() {
        Recipe r = new Recipe(1, "R", "").add("flour", 500, "g");
        CookPlanner.Plan plan = CookPlanner.plan(r, Arrays.asList(item(1, "flour", 1, "kg", null)));
        assertEquals(0.5, change(plan, 1).newQuantity, 1e-9);          // 1 kg - 500 g = 0.5 kg
    }

    @Test
    public void usesTheSoonestExpiringItemFirstAndSpillsOverToTheNext() {
        Recipe r = new Recipe(1, "R", "").add("milk", 600, "ml");
        List<PantryItem> pantry = Arrays.asList(
                item(1, "milk", 1, "l", "2026-12-01"),
                item(2, "milk", 500, "ml", "2026-10-20"),     // expires first, only 500 ml
                item(3, "milk", 2, "l", null));               // no date -> used last
        CookPlanner.Plan plan = CookPlanner.plan(r, pantry);
        assertTrue(plan.possible);
        assertTrue(change(plan, 2).delete);                           // all 500 ml used
        assertEquals(0.9, change(plan, 1).newQuantity, 1e-9);        // 100 ml taken from the 1 l
        assertEquals(null, change(plan, 3));                          // untouched
    }

    @Test
    public void floatingPointNoiseDoesNotLeaveGhostItems() {
        Recipe r = new Recipe(1, "R", "").add("sugar", 0.1, "kg");
        CookPlanner.Plan plan = CookPlanner.plan(r, Arrays.asList(item(1, "sugar", 100, "g", null)));
        assertTrue(change(plan, 1).delete);
    }

    @Test
    public void notEnoughStockMeansNoChangesAtAll() {
        Recipe r = new Recipe(1, "R", "").add("egg", 2, "pcs").add("flour", 500, "g");
        List<PantryItem> pantry = Arrays.asList(
                item(1, "egg", 6, "pcs", null), item(2, "flour", 100, "g", null));
        CookPlanner.Plan plan = CookPlanner.plan(r, pantry);
        assertFalse(plan.possible);
        assertTrue(plan.changes.isEmpty());                // the eggs are not touched either
        assertEquals(Arrays.asList("flour"), plan.missing);
    }

    @Test
    public void ingredientMissingEntirelyIsReported() {
        Recipe r = new Recipe(1, "R", "").add("saffron", 1, "g");
        CookPlanner.Plan plan = CookPlanner.plan(r, Arrays.asList(item(1, "egg", 6, "pcs", null)));
        assertFalse(plan.possible);
        assertEquals(Arrays.asList("saffron"), plan.missing);
    }

    @Test
    public void synonymsAreDeductedFromTheMatchingItem() {
        Recipe r = new Recipe(1, "R", "").add("eggplant", 1, "pcs");
        CookPlanner.Plan plan = CookPlanner.plan(r, Arrays.asList(item(1, "Aubergines", 3, "pcs", null)));
        assertEquals(2, change(plan, 1).newQuantity, 1e-9);
    }
}
