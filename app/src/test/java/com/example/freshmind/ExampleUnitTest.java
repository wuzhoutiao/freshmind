package com.example.freshmind;

import org.junit.Test;

import com.example.freshmind.model.InventoryItem;
import com.example.freshmind.model.recipe.Recipe;
import com.example.freshmind.service.RecipeService;


import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() {
        assertEquals(4, 2 + 2);
    }

    // test RecipeService
    @Test
    public void recommendRecipes_shouldRecommendMatchedRecipe() {
        List<InventoryItem> inventory = new ArrayList<>();
        inventory.add(new InventoryItem(1, "egg", "meat", "2", "2026-05-01",
                "2026-05-07", InventoryItem.STATUS_NEAR_EXPIRY));
        inventory.add(new InventoryItem(2, "tomato", "Vegetables", "3", "2026-05-02",
                "2026-05-10", InventoryItem.STATUS_NORMAL));

        Recipe recipe = new Recipe("Tomato Egg");
        recipe.addIngredient("egg", 2, "pcs");
        recipe.addIngredient("tomato", 1, "pcs");

        List<Recipe> recipes = new ArrayList<>();
        recipes.add(recipe);

        RecipeService service = new RecipeService();
        List<Recipe> result = service.recommendRecipes(inventory, recipes);

        assertEquals(1, result.size());
        assertEquals("Tomato Egg", result.get(0).getName());
    }
}