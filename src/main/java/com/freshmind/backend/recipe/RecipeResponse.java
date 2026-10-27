package com.freshmind.backend.recipe;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record RecipeResponse(
        UUID id,
        String name,
        String description,
        int cookingMinutes,
        String imageKey,
        List<String> steps,
        List<IngredientAmount> ingredients) {

    public record IngredientAmount(String name, BigDecimal amount, String unit) {
    }
}

