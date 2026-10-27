package com.example.smartpantry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FiltersAndUnitsTest {

    // ---- pantry search -----------------------------------------------------------------------

    private static List<PantryItem> pantry() {
        return Arrays.asList(
                new PantryItem(1, "Tomatoes", 4, "pcs", null),
                new PantryItem(2, "Eggplant", 1, "pcs", null),
                new PantryItem(3, "Olive oil", 500, "ml", null),
                new PantryItem(4, "Milk", 1, "l", null));
    }

    @Test
    public void blankQueryReturnsEverything() {
        assertEquals(4, Filters.pantry(pantry(), "  ").size());
        assertEquals(4, Filters.pantry(pantry(), null).size());
    }

    @Test
    public void searchIsCaseInsensitiveAndPartial() {
        assertEquals(1, Filters.pantry(pantry(), "OLIVE").size());
        assertEquals(2, Filters.pantry(pantry(), "il").size());          // "Olive oil" and "Milk"
    }

    @Test
    public void searchFindsPluralAndSynonymNames() {
        assertEquals("Tomatoes", Filters.pantry(pantry(), "tomato").get(0).name);
        assertEquals("Eggplant", Filters.pantry(pantry(), "aubergine").get(0).name);
        assertTrue(Filters.pantry(pantry(), "saffron").isEmpty());
    }

    // ---- suggestion filters ------------------------------------------------------------------

    private static MatchResult result(String name, int minutes, boolean custom, int expiring) {
        Recipe r = new Recipe(0, name, "");
        r.minutes = minutes;
        r.custom = custom;
        MatchResult m = new MatchResult(r);
        m.expiringUsed = expiring;
        return m;
    }

    @Test
    public void quickFilterKeepsOnlyRecipesUnderTheLimit() {
        List<MatchResult> in = Arrays.asList(result("fast", 10, false, 0), result("edge", 20, false, 0),
                result("slow", 45, false, 0));
        List<MatchResult> out = Filters.suggestions(in, true, false, false);
        assertEquals(2, out.size());
        assertEquals("fast", out.get(0).recipe.name);
        assertEquals("edge", out.get(1).recipe.name);
    }

    @Test
    public void filtersCombineWithAnd() {
        List<MatchResult> in = new ArrayList<>(Arrays.asList(
                result("quick+expiring", 10, false, 2),
                result("quick only", 10, false, 0),
                result("expiring only", 40, false, 1),
                result("mine, quick, expiring", 15, true, 1)));
        assertEquals(2, Filters.suggestions(in, true, true, false).size());
        assertEquals(1, Filters.suggestions(in, true, true, true).size());
        assertEquals(4, Filters.suggestions(in, false, false, false).size());
    }

    // ---- units / dates -----------------------------------------------------------------------

    @Test
    public void unitConversion() {
        assertEquals(1000, Units.toBase(1, "kg"), 1e-9);
        assertEquals(720, Units.toBase(3, "cup"), 1e-9);                // 3 cups = 720 ml
        assertEquals("mass", Units.family("KG"));
        assertEquals("volume", Units.family("tbsp"));
        assertEquals("count", Units.family("pieces"));
        assertEquals("other:bunch", Units.family("Bunch"));
    }

    @Test
    public void quantityFormatting() {
        assertEquals("2", Units.format(2.0));
        assertEquals("0.5", Units.format(0.5));
        assertEquals("1.33", Units.format(1.3333));
        assertEquals("150 g", Units.describe(150, "g"));
    }

    @Test
    public void daysUntilExpiry() {
        long today = DateUtil.epochDay(2026, 10, 19);
        assertEquals(Long.valueOf(0), DateUtil.daysUntil("2026-10-19", today));
        assertEquals(Long.valueOf(1), DateUtil.daysUntil("2026-10-20", today));
        assertEquals(Long.valueOf(-4), DateUtil.daysUntil("2026-10-15", today));
        assertEquals(Long.valueOf(30), DateUtil.daysUntil("2026-11-18", today));  // crosses a month end
        assertNull(DateUtil.daysUntil(null, today));
        assertNull(DateUtil.daysUntil("", today));
        assertNull(DateUtil.daysUntil("garbage", today));
    }

    // ---- validation / wording ----------------------------------------------------------------

    @Test
    public void validationRejectsBadInput() {
        assertEquals("Olive oil", Validation.name("  Olive   oil "));
        assertNull(Validation.name("   "));
        assertNull(Validation.name("12345"));
        assertEquals(Double.valueOf(1.5), Validation.quantity("1,5"));
        assertNull(Validation.quantity("0"));
        assertNull(Validation.quantity("-2"));
        assertNull(Validation.quantity("abc"));
        assertNull(Validation.quantity(""));
        assertEquals(Integer.valueOf(20), Validation.minutes(" 20 "));
        assertNull(Validation.minutes("0"));
        assertNull(Validation.minutes("2.5"));
        assertTrue(Validation.unit("KG"));
        assertTrue(!Validation.unit("bunch"));
    }

    @Test
    public void expiryWording() {
        assertEquals("Expired 3 days ago", DateUtil.relative(-3));
        assertEquals("Expired yesterday", DateUtil.relative(-1));
        assertEquals("Expires today", DateUtil.relative(0));
        assertEquals("Expires tomorrow", DateUtil.relative(1));
        assertEquals("Expires in 5 days", DateUtil.relative(5));
        assertEquals("2026-10-19", DateUtil.formatEpochDay(DateUtil.epochDay(2026, 10, 19)));
    }
}
