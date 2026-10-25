package com.freshmind.backend.ai;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class GeneratedRecipeCache {

    private final RecipeGenerationClient client;

    public GeneratedRecipeCache(RecipeGenerationClient client) {
        this.client = client;
    }

    @Cacheable(
            cacheNames = "aiRecipeCandidates",
            key = "#p0.toString() + ':' + #p1 + ':' + #p2",
            unless = "#result == null || #result.isEmpty()")
    public List<GeneratedRecipe> getOrGenerate(
            UUID ownerId,
            String inventoryFingerprint,
            String modelAndPromptVersion,
            List<PantryIngredient> pantry,
            List<String> allowedIngredients) {
        List<GeneratedRecipe> generated = client.generate(pantry, allowedIngredients);
        if (generated == null) {
            return List.of();
        }
        List<GeneratedRecipe> cacheSafe = new ArrayList<>();
        for (GeneratedRecipe recipe : generated) {
            if (recipe == null) {
                cacheSafe.add(null);
                continue;
            }
            cacheSafe.add(new GeneratedRecipe(
                    recipe.name(),
                    recipe.description(),
                    recipe.cookingMinutes(),
                    recipe.ingredients() == null ? null : new ArrayList<>(recipe.ingredients()),
                    recipe.steps() == null ? null : new ArrayList<>(recipe.steps())));
        }
        return cacheSafe;
    }
}
