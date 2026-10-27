package com.example.smartpantry;

import java.util.HashMap;
import java.util.Map;

/**
 * Regional / alternative ingredient names mapped to one canonical name, so a pantry entry of
 * "aubergine" satisfies a recipe that asks for "eggplant". Keys are singular, lower-case.
 * Add new pairs in the static block; everything else picks them up automatically.
 */
public final class Synonyms {
    private Synonyms() {}

    /** Single-word synonyms (checked per word, after singularising). */
    private static final Map<String, String> WORDS = new HashMap<>();
    /** Multi-word synonyms (checked against the whole normalised name). */
    private static final Map<String, String> PHRASES = new HashMap<>();

    static {
        // UK / SA / other English  ->  US-style canonical name
        word("aubergine", "eggplant");
        word("brinjal", "eggplant");
        word("coriander", "cilantro");
        word("courgette", "zucchini");
        word("capsicum", "bell pepper");
        word("rocket", "arugula");
        word("prawn", "shrimp");
        word("scallion", "green onion");
        word("beetroot", "beet");
        word("cornflour", "cornstarch");
        word("yoghurt", "yogurt");
        word("garbanzo", "chickpea");
        word("swede", "rutabaga");
        word("maize", "corn");
        word("mangetout", "snow pea");
        // spelling variants; "chilly"/"chily" are what plural "chillies"/"chilies" singularise to
        word("chilli", "chili");
        word("chilly", "chili");
        word("chily", "chili");
        word("chile", "chili");

        phrase("spring onion", "green onion");
        phrase("minced beef", "ground beef");
        phrase("beef mince", "ground beef");
        phrase("icing sugar", "powdered sugar");
        phrase("double cream", "heavy cream");
        phrase("single cream", "light cream");
        phrase("bicarbonate of soda", "baking soda");
    }

    private static void word(String from, String to) { WORDS.put(from, to); }
    private static void phrase(String from, String to) { PHRASES.put(from, to); }

    public static String mapWord(String singularWord) {
        String m = WORDS.get(singularWord);
        return m != null ? m : singularWord;
    }

    public static String mapPhrase(String phrase) {
        String m = PHRASES.get(phrase);
        return m != null ? m : phrase;
    }
}
