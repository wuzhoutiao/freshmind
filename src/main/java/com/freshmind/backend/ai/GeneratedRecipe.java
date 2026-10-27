package com.freshmind.backend.ai;

import java.util.List;

public record GeneratedRecipe(
        String name,
        String description,
        int cookingMinutes,
        List<GeneratedIngredient> ingredients,
        List<String> steps) {
}
