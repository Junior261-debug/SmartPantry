package com.example.smartpantry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.List;

public class ValidationAndSchemaTest {

    @Test
    public void nameValidation() {
        assertEquals("olive oil", Validation.name("  olive   oil "));
        assertNull(Validation.name(""));
        assertNull(Validation.name("   "));
        assertNull(Validation.name("12345"));                 // no letters
        assertNull(Validation.name(new String(new char[61]).replace('\0', 'a')));
        assertNull(Validation.name(null));
    }

    @Test
    public void quantityValidation() {
        assertEquals(2.5, Validation.quantity("2.5"), 1e-9);
        assertEquals(2.5, Validation.quantity("2,5"), 1e-9);  // comma decimal
        assertNull(Validation.quantity("0"));
        assertNull(Validation.quantity("-3"));
        assertNull(Validation.quantity("abc"));
        assertNull(Validation.quantity(""));
        assertNull(Validation.quantity("9999999"));
    }

    @Test
    public void minutesAndUnitValidation() {
        assertEquals(Integer.valueOf(25), Validation.minutes("25"));
        assertNull(Validation.minutes("0"));
        assertNull(Validation.minutes("1441"));
        assertNull(Validation.minutes("1.5"));
        assertTrue(Validation.unit("kg"));
        assertTrue(Validation.unit("Tbsp"));
        assertFalse(Validation.unit("bunch"));
        assertFalse(Validation.unit(""));
    }

    @Test
    public void migrationFromV1ContainsTheV2Steps() {
        List<String> up = Schema.upgrade(1, 2);
        assertTrue(up.get(0).startsWith("ALTER TABLE recipes ADD COLUMN minutes"));
        assertTrue(up.get(1).startsWith("ALTER TABLE recipes ADD COLUMN is_custom"));
        assertEquals(Schema.SEED_MINUTES.size(), countStartingWith(up, "UPDATE recipes SET minutes"));
        assertTrue(Schema.upgrade(2, 2).isEmpty());           // already current: nothing to do
    }

    @Test
    public void seedCollectionHasAtLeastFifteenRecipes() {
        assertTrue(Schema.SEED_MINUTES.size() >= 15);
    }

    private static int countStartingWith(List<String> l, String prefix) {
        int n = 0;
        for (String s : l) if (s.startsWith(prefix)) n++;
        return n;
    }
}
