package com.freshmind.backend.ai;

import java.util.List;

public interface RecipeGenerationClient {

    List<GeneratedRecipe> generate(
            List<PantryIngredient> pantry,
            List<String> allowedIngredients);
}
