package com.freshmind.backend.catalog;

public record CatalogIngredientResponse(
        long id,
        String name,
        IngredientCategory category,
        int defaultShelfLifeDays) {
}

