package com.example.freshmind.data;

import com.example.freshmind.model.Ingredient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Static inventory ingredient data used as the base catalog.
 */
public final class InventoryDatabase {

    private static final List<Ingredient> INGREDIENTS = buildIngredients();

    private InventoryDatabase() {
    }

    /**
     * Returns all available ingredients in the base inventory catalog.
     */
    public static List<Ingredient> getIngredients() {
        return Collections.unmodifiableList(INGREDIENTS);
    }

    /**
     * Updates the default shelf life for an ingredient by its catalog ID.
     */
    public static boolean updateDefaultShelfLifeDays(int ingredientId, int days) {
        for (Ingredient ingredient : INGREDIENTS) {
            if (ingredient.getIngredientId() == ingredientId) {
                ingredient.setDefaultShelfLifeDays(days);
                return true;
            }
        }
        return false;
    }

    /**
     * Builds the static ingredient catalog used by inventory and recipe features.
     */
    private static List<Ingredient> buildIngredients() {
        List<Ingredient> items = new ArrayList<>();
        items.add(new Ingredient(1, "Rice", "Others", 365));
        items.add(new Ingredient(2, "Noodles", "Others", 180));
        items.add(new Ingredient(3, "Bread", "Others", 7));
        items.add(new Ingredient(4, "Egg", "Others", 21));
        items.add(new Ingredient(5, "Chicken Breast", "Meat", 2));
        items.add(new Ingredient(6, "Beef", "Meat", 3));
        items.add(new Ingredient(7, "Salmon", "Meat", 2));
        items.add(new Ingredient(8, "Tofu", "Others", 5));
        items.add(new Ingredient(9, "Shrimp", "Meat", 2));
        items.add(new Ingredient(10, "Milk", "Others", 7));
        items.add(new Ingredient(11, "Cheddar Cheese", "Others", 21));
        items.add(new Ingredient(12, "Yogurt", "Others", 14));
        items.add(new Ingredient(13, "Butter", "Others", 30));
        items.add(new Ingredient(14, "Tomato", "Vegetables", 7));
        items.add(new Ingredient(15, "Potato", "Vegetables", 30));
        items.add(new Ingredient(16, "Onion", "Vegetables", 30));
        items.add(new Ingredient(17, "Garlic", "Vegetables", 60));
        items.add(new Ingredient(18, "Carrot", "Vegetables", 21));
        items.add(new Ingredient(19, "Broccoli", "Vegetables", 7));
        items.add(new Ingredient(20, "Spinach", "Vegetables", 5));
        items.add(new Ingredient(21, "Mushroom", "Vegetables", 7));
        items.add(new Ingredient(22, "Bell Pepper", "Vegetables", 10));
        items.add(new Ingredient(23, "Cucumber", "Vegetables", 7));
        items.add(new Ingredient(24, "Lettuce", "Vegetables", 5));
        items.add(new Ingredient(25, "Apple", "Fruits", 30));
        items.add(new Ingredient(26, "Banana", "Fruits", 7));
        items.add(new Ingredient(27, "Lemon", "Fruits", 14));
        items.add(new Ingredient(28, "Olive Oil", "Others", 180));
        items.add(new Ingredient(29, "Soy Sauce", "Others", 180));
        items.add(new Ingredient(30, "Black Pepper", "Others", 365));
        return items;
    }
}
