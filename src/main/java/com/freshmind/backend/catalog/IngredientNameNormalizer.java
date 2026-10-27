package com.freshmind.backend.catalog;

import java.util.Locale;

public final class IngredientNameNormalizer {

    private IngredientNameNormalizer() {
    }

    public static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }
}

