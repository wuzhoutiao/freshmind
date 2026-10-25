package com.example.freshmind.service;

import com.example.freshmind.model.InventoryItem;
import com.example.freshmind.model.recipe.Recipe;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Provides recipe recommendation logic based on the user's current inventory.
 */
public class RecipeService {

    private static final double RECOMMEND_THRESHOLD = 0.6; //A recipe will be recommended if its match rate is at least 60%.
    private static final double NEAR_EXPIRY_BONUS = 0.2; //Recipes using near-expiry ingredients will be ranked higher.

    /**
     * Recommends recipes that match enough inventory ingredients and prioritizes near-expiry items.
     */
    public List<Recipe> recommendRecipes(List<InventoryItem> inventoryItems,
                                         List<Recipe> recipes) {

        List<RecipeScore> scoredRecipes = new ArrayList<>();

        if (inventoryItems == null || recipes == null) {
            return new ArrayList<>();
        }

        Set<String> userIngredientNames = getInventoryIngredientNames(inventoryItems);
        Set<String> nearExpiryIngredients = getNearExpiryIngredients(inventoryItems);

        for (Recipe recipe : recipes) {
            double matchRate = calculateMatchRate(recipe, userIngredientNames);

            if (matchRate >= RECOMMEND_THRESHOLD) {
                int nearExpiryCount = countNearExpiryIngredients(recipe, nearExpiryIngredients);

                double finalScore = matchRate + nearExpiryCount * NEAR_EXPIRY_BONUS;

                scoredRecipes.add(new RecipeScore(recipe, finalScore));
            }
        }

        scoredRecipes.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));

        List<Recipe> recommendedRecipes = new ArrayList<>();

        for (RecipeScore recipeScore : scoredRecipes) {
            recommendedRecipes.add(recipeScore.getRecipe());
        }

        return recommendedRecipes;
    }


    /**
     * Calculates the percentage of a recipe's required ingredients that exist in the inventory.
     */
    public double calculateMatchRate(Recipe recipe,
                                     List<InventoryItem> inventoryItems) {

        if (recipe == null || inventoryItems == null) {
            return 0.0;
        }

        if (recipe.getIngredientsNeeded().isEmpty()) {
            return 0.0;
        }

        return calculateMatchRate(recipe, getInventoryIngredientNames(inventoryItems));
    }

    private double calculateMatchRate(Recipe recipe,
                                      Set<String> userIngredientNames) {

        if (recipe == null || userIngredientNames == null || recipe.getIngredientsNeeded().isEmpty()) {
            return 0.0;
        }

        int matchedCount = 0;

        for (String requiredIngredientName : recipe.getIngredientsNeeded().keySet()) {
            if (userIngredientNames.contains(normalize(requiredIngredientName))) {
                matchedCount++;
            }
        }

        return (double) matchedCount / recipe.getIngredientsNeeded().size();
    }

    private Set<String> getInventoryIngredientNames(List<InventoryItem> inventoryItems) {
        Set<String> userIngredientNames = new HashSet<>();

        for (InventoryItem item : inventoryItems) {
            userIngredientNames.add(normalize(item.getIngredientName()));
        }

        return userIngredientNames;
    }

    /**
     * Returns the recipe ingredients that are not currently available in the inventory.
     */
    public List<String> getMissingIngredients(Recipe recipe,
                                              List<InventoryItem> inventoryItems) {

        List<String> missingIngredients = new ArrayList<>();

        if (recipe == null || inventoryItems == null) {
            return missingIngredients;
        }

        Set<String> userIngredientNames = new HashSet<>();

        for (InventoryItem item : inventoryItems) {
            userIngredientNames.add(normalize(item.getIngredientName()));
        }

        for (String requiredIngredientName : recipe.getIngredientsNeeded().keySet()) {
            if (!userIngredientNames.contains(normalize(requiredIngredientName))) {
                missingIngredients.add(requiredIngredientName);
            }
        }

        return missingIngredients;
    }


    /**
     * Collects normalized ingredient names for inventory items that are near expiry.
     */
    private Set<String> getNearExpiryIngredients(List<InventoryItem> inventoryItems) {

        Set<String> nearExpiryIngredients = new HashSet<>();

        for (InventoryItem item : inventoryItems) {
            if (InventoryItem.STATUS_NEAR_EXPIRY.equals(item.getStatus())) {
                nearExpiryIngredients.add(normalize(item.getIngredientName()));
            }
        }

        return nearExpiryIngredients;
    }


    /**
     * Counts how many required recipe ingredients are near expiry in the inventory.
     */
    private int countNearExpiryIngredients(Recipe recipe,
                                           Set<String> nearExpiryIngredients) {

        int count = 0;

        for (String requiredIngredientName : recipe.getIngredientsNeeded().keySet()) {
            if (nearExpiryIngredients.contains(normalize(requiredIngredientName))) {
                count++;
            }
        }

        return count;
    }

    /**
     * Normalizes ingredient names for case-insensitive comparisons.
     */
    private String normalize(String name) {
        if (name == null) {
            return "";
        }
        return name.trim().toLowerCase();
    }

    /**
     * Stores a recipe with its recommendation score for sorting.
     */
    private static class RecipeScore {
        private final Recipe recipe;
        private final double score;

        public RecipeScore(Recipe recipe, double score) {
            this.recipe = recipe;
            this.score = score;
        }

        public Recipe getRecipe() {
            return recipe;
        }

        public double getScore() {
            return score;
        }
    }
}
