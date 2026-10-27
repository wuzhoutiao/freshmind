package com.freshmind.backend.recipe;

import java.util.List;

public record RecommendationResponse(
        RecipeResponse recipe,
        double matchRate,
        double score,
        List<String> matchedIngredients,
        List<String> missingIngredients,
        List<String> nearExpiryIngredientsUsed,
        String explanation) {
}

