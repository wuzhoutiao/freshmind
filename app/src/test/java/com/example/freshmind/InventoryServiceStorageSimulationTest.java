package com.example.freshmind;

import com.example.freshmind.data.InventoryDatabase;
import com.example.freshmind.data.RecipeDatabase;
import com.example.freshmind.model.Ingredient;
import com.example.freshmind.model.InventoryItem;
import com.example.freshmind.model.recipe.Recipe;
import com.example.freshmind.service.ExpiryReminderService;
import com.example.freshmind.service.InventoryService;
import com.example.freshmind.service.RecipeService;

import org.junit.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class InventoryServiceStorageSimulationTest {

    @Test
    public void testStorageSeed_shouldLoadThirtyTwoItemsIntoInventoryService() {
        InventoryService service = createInventoryServiceWithStorageSeed();

        assertEquals(32, service.getItemCount());
        assertEquals(33, service.getNextAvailableId());
        assertNotNull(service.findItemById(1));
        assertEquals("Pork", service.findItemById(32).getIngredientName());
    }

    @Test
    public void testStorageSeed_shouldCoverDatabaseAndCustomIngredients() {
        List<InventoryItem> items = TestInventoryStorageData.createStorageItems();

        assertTrue(containsCatalogIngredient(items));
        assertTrue(containsCustomIngredient(items));
        assertTrue(containsRecipeIngredient(items));
        assertTrue(containsNonRecipeIngredient(items));
    }

    @Test
    public void testStorageSeed_shouldCoverExpiryRangeFromOneDayToTwoYears() {
        ExpiryReminderService reminderService = new ExpiryReminderService();
        List<InventoryItem> items = TestInventoryStorageData.createStorageItems();

        assertEquals(1, reminderService.getDaysUntilExpiry(findByName(items, "Egg"),
                TestInventoryStorageData.BASE_TODAY));
        assertEquals(2, reminderService.getDaysUntilExpiry(findByName(items, "Tomato"),
                TestInventoryStorageData.BASE_TODAY));
        assertEquals(731, reminderService.getDaysUntilExpiry(findByName(items, "Rice"),
                TestInventoryStorageData.BASE_TODAY));
        assertEquals(7, reminderService.getNearExpiryItems(items,
                TestInventoryStorageData.BASE_TODAY).size());
        assertEquals(2, reminderService.getExpiredItems(items,
                TestInventoryStorageData.BASE_TODAY).size());
    }

    @Test
    public void testStorageSeed_shouldSupportInventoryFilteringAndSorting() {
        InventoryService service = createInventoryServiceWithStorageSeed();

        assertEquals(5, service.getItemsByCategory("meat").size());
        assertEquals(9, service.getItemsByCategory("vegetables").size());
        assertEquals("Pork", service.getItemsSortedByExpiryDate().get(0).getIngredientName());
        assertEquals("Black Pepper", service.findItemsByName(" black pepper ").get(0).getIngredientName());
    }

    @Test
    public void testStorageSeed_shouldExerciseRecipeRecommendationFlow() {
        InventoryService service = createInventoryServiceWithStorageSeed();
        RecipeService recipeService = new RecipeService();

        List<Recipe> recommendations = recipeService.recommendRecipes(
                service.getAllItems(),
                RecipeDatabase.getRecipes()
        );

        assertFalse(recommendations.isEmpty());
        assertTrue(containsRecipe(recommendations, "Tomato Egg Fried Rice"));
        assertTrue(containsRecipe(recommendations, "Chicken Broccoli Stir-Fry"));
    }

    private InventoryService createInventoryServiceWithStorageSeed() {
        return new InventoryService(TestInventoryStorageData.createSeed().getItems());
    }

    private boolean containsCatalogIngredient(List<InventoryItem> items) {
        Set<String> catalogNames = new HashSet<>();
        for (Ingredient ingredient : InventoryDatabase.getIngredients()) {
            catalogNames.add(normalize(ingredient.getIngredientName()));
        }

        for (InventoryItem item : items) {
            if (catalogNames.contains(normalize(item.getIngredientName()))) {
                return true;
            }
        }
        return false;
    }

    private boolean containsCustomIngredient(List<InventoryItem> items) {
        Set<String> catalogNames = new HashSet<>();
        for (Ingredient ingredient : InventoryDatabase.getIngredients()) {
            catalogNames.add(normalize(ingredient.getIngredientName()));
        }

        for (InventoryItem item : items) {
            if (!catalogNames.contains(normalize(item.getIngredientName()))) {
                return true;
            }
        }
        return false;
    }

    private boolean containsRecipeIngredient(List<InventoryItem> items) {
        Set<String> recipeIngredientNames = getRecipeIngredientNames();
        for (InventoryItem item : items) {
            if (recipeIngredientNames.contains(normalize(item.getIngredientName()))) {
                return true;
            }
        }
        return false;
    }

    private boolean containsNonRecipeIngredient(List<InventoryItem> items) {
        Set<String> recipeIngredientNames = getRecipeIngredientNames();
        for (InventoryItem item : items) {
            if (!recipeIngredientNames.contains(normalize(item.getIngredientName()))) {
                return true;
            }
        }
        return false;
    }

    private Set<String> getRecipeIngredientNames() {
        Set<String> names = new HashSet<>();
        for (Recipe recipe : RecipeDatabase.getRecipes()) {
            for (String ingredientName : recipe.getIngredientsNeeded().keySet()) {
                names.add(normalize(ingredientName));
            }
        }
        return names;
    }

    private InventoryItem findByName(List<InventoryItem> items, String name) {
        for (InventoryItem item : items) {
            if (normalize(item.getIngredientName()).equals(normalize(name))) {
                return item;
            }
        }
        throw new AssertionError("Missing test inventory item: " + name);
    }

    private boolean containsRecipe(List<Recipe> recipes, String name) {
        for (Recipe recipe : recipes) {
            if (normalize(recipe.getName()).equals(normalize(name))) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String value) {
        return value.trim().toLowerCase();
    }
}
