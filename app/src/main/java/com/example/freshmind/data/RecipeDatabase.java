package com.example.freshmind.data;

import com.example.freshmind.model.recipe.Quantity;
import com.example.freshmind.model.recipe.Recipe;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Static recipe data seeded from the inventory ingredient catalog.
 */
public final class RecipeDatabase {
    private static final List<Recipe> RECIPES = Collections.unmodifiableList(buildRecipes());

    private RecipeDatabase() {
    }

    /**
     * Returns the static list of recipes available in the app.
     */
    public static List<Recipe> getRecipes() {
        return RECIPES;
    }

    private static List<Recipe> buildRecipes() {
        List<Recipe> recipes = new ArrayList<>();
        recipes.add(recipe(
                "Tomato Egg Fried Rice",
                "A quick fried rice using common fridge staples.",
                15,
                ingredients(
                        item("Rice", 1, "bowl"),
                        item("Egg", 2, "pcs"),
                        item("Tomato", 1, "pc"),
                        item("Garlic", 2, "cloves"),
                        item("Olive Oil", 1, "tbsp"),
                        item("Soy Sauce", 1, "tbsp"),
                        item("Black Pepper", 1, "tsp")
                ),
                "Beat the eggs and dice the tomato.",
                "Saute garlic with olive oil, then cook the eggs until softly set.",
                "Add tomato and rice, season with soy sauce and black pepper, then stir-fry well."
        ));
        recipes.add(recipe(
                "Chicken Broccoli Stir-Fry",
                "Lean chicken and broccoli in a light savory sauce.",
                20,
                ingredients(
                        item("Chicken Breast", 200, "g"),
                        item("Broccoli", 150, "g"),
                        item("Garlic", 2, "cloves"),
                        item("Soy Sauce", 2, "tbsp"),
                        item("Olive Oil", 1, "tbsp"),
                        item("Black Pepper", 1, "tsp")
                ),
                "Slice the chicken breast and broccoli into bite-sized pieces.",
                "Sear chicken in olive oil until almost cooked through.",
                "Add garlic and broccoli, then finish with soy sauce and black pepper."
        ));
        recipes.add(recipe(
                "Creamy Mushroom Pasta",
                "Comfort pasta with milk, cheese, and mushrooms.",
                25,
                ingredients(
                        item("Noodles", 200, "g"),
                        item("Mushroom", 120, "g"),
                        item("Milk", 200, "ml"),
                        item("Cheddar Cheese", 50, "g"),
                        item("Butter", 1, "tbsp"),
                        item("Garlic", 2, "cloves"),
                        item("Black Pepper", 1, "tsp")
                ),
                "Cook the noodles until tender and drain.",
                "Saute mushroom and garlic in butter.",
                "Add milk and cheese, then toss in noodles and black pepper."
        ));
        recipes.add(recipe(
                "Beef Potato Skillet",
                "A hearty skillet meal with tender potatoes and onions.",
                30,
                ingredients(
                        item("Beef", 220, "g"),
                        item("Potato", 2, "pcs"),
                        item("Onion", 1, "pc"),
                        item("Garlic", 2, "cloves"),
                        item("Olive Oil", 1, "tbsp"),
                        item("Black Pepper", 1, "tsp")
                ),
                "Dice the potatoes and onion.",
                "Brown the beef with garlic in olive oil.",
                "Add potatoes and onion, cover, and cook until tender."
        ));
        recipes.add(recipe(
                "Salmon Lemon Rice Bowl",
                "Bright salmon bowl with rice and greens.",
                20,
                ingredients(
                        item("Salmon", 180, "g"),
                        item("Rice", 1, "bowl"),
                        item("Spinach", 80, "g"),
                        item("Lemon", 0.5, "pc"),
                        item("Olive Oil", 1, "tbsp"),
                        item("Black Pepper", 1, "tsp")
                ),
                "Pan-sear the salmon with olive oil and black pepper.",
                "Warm the rice and wilt the spinach.",
                "Assemble the bowl and finish with lemon juice."
        ));
        recipes.add(recipe(
                "Tofu Tomato Soup",
                "Light tofu soup with tomato and egg.",
                18,
                ingredients(
                        item("Tofu", 200, "g"),
                        item("Tomato", 2, "pcs"),
                        item("Egg", 1, "pc"),
                        item("Garlic", 1, "clove"),
                        item("Black Pepper", 1, "tsp")
                ),
                "Cut tofu and tomato into chunks.",
                "Simmer tomato and garlic until the broth turns rich.",
                "Add tofu, drizzle in egg, and season with black pepper."
        ));
        recipes.add(recipe(
                "Shrimp Garlic Noodles",
                "Fast noodles with shrimp and garlic.",
                15,
                ingredients(
                        item("Shrimp", 180, "g"),
                        item("Noodles", 180, "g"),
                        item("Garlic", 3, "cloves"),
                        item("Soy Sauce", 1.5, "tbsp"),
                        item("Olive Oil", 1, "tbsp"),
                        item("Black Pepper", 1, "tsp")
                ),
                "Cook the noodles and drain.",
                "Stir-fry shrimp and garlic in olive oil.",
                "Add noodles and soy sauce, then toss together."
        ));
        recipes.add(recipe(
                "Spinach Omelette",
                "Protein-rich omelette with spinach and cheese.",
                10,
                ingredients(
                        item("Egg", 3, "pcs"),
                        item("Spinach", 60, "g"),
                        item("Cheddar Cheese", 30, "g"),
                        item("Butter", 1, "tsp"),
                        item("Black Pepper", 1, "tsp")
                ),
                "Whisk the eggs with black pepper.",
                "Cook spinach briefly in butter.",
                "Pour in eggs, add cheese, and fold when set."
        ));
        recipes.add(recipe(
                "Chicken Lettuce Wraps",
                "Fresh wraps with savory chicken filling.",
                20,
                ingredients(
                        item("Chicken Breast", 180, "g"),
                        item("Lettuce", 6, "leaves"),
                        item("Onion", 0.5, "pc"),
                        item("Garlic", 2, "cloves"),
                        item("Soy Sauce", 1, "tbsp"),
                        item("Olive Oil", 1, "tbsp")
                ),
                "Mince the chicken, onion, and garlic.",
                "Cook the filling in olive oil and season with soy sauce.",
                "Spoon the hot filling into lettuce leaves."
        ));
        recipes.add(recipe(
                "Broccoli Cheese Bake",
                "Simple baked broccoli with a cheesy topping.",
                25,
                ingredients(
                        item("Broccoli", 200, "g"),
                        item("Cheddar Cheese", 80, "g"),
                        item("Milk", 80, "ml"),
                        item("Butter", 1, "tbsp"),
                        item("Black Pepper", 1, "tsp")
                ),
                "Steam the broccoli until just tender.",
                "Mix milk, cheese, butter, and black pepper.",
                "Bake the broccoli with the sauce until bubbling."
        ));
        recipes.add(recipe(
                "Beef Onion Rice",
                "Savory beef bowl with sweet onion over rice.",
                22,
                ingredients(
                        item("Beef", 200, "g"),
                        item("Rice", 1, "bowl"),
                        item("Onion", 1, "pc"),
                        item("Soy Sauce", 1.5, "tbsp"),
                        item("Olive Oil", 1, "tbsp"),
                        item("Black Pepper", 1, "tsp")
                ),
                "Slice the beef and onion thinly.",
                "Saute onion in olive oil, then add beef.",
                "Season with soy sauce and serve over rice."
        ));
        recipes.add(recipe(
                "Tomato Cucumber Salad",
                "Refreshing salad with lemon dressing.",
                8,
                ingredients(
                        item("Tomato", 2, "pcs"),
                        item("Cucumber", 1, "pc"),
                        item("Lettuce", 80, "g"),
                        item("Lemon", 0.5, "pc"),
                        item("Olive Oil", 1, "tbsp"),
                        item("Black Pepper", 0.5, "tsp")
                ),
                "Slice the tomato and cucumber.",
                "Tear the lettuce and arrange in a bowl.",
                "Dress with lemon juice, olive oil, and black pepper."
        ));
        recipes.add(recipe(
                "Garlic Butter Shrimp",
                "Juicy shrimp cooked in garlic butter.",
                12,
                ingredients(
                        item("Shrimp", 200, "g"),
                        item("Garlic", 3, "cloves"),
                        item("Butter", 1, "tbsp"),
                        item("Lemon", 0.5, "pc"),
                        item("Black Pepper", 1, "tsp")
                ),
                "Pat the shrimp dry.",
                "Cook garlic in butter, then add the shrimp.",
                "Finish with lemon juice and black pepper."
        ));
        recipes.add(recipe(
                "Tofu Broccoli Bowl",
                "Balanced tofu bowl with rice and vegetables.",
                18,
                ingredients(
                        item("Tofu", 200, "g"),
                        item("Broccoli", 120, "g"),
                        item("Rice", 1, "bowl"),
                        item("Soy Sauce", 1, "tbsp"),
                        item("Olive Oil", 1, "tbsp"),
                        item("Garlic", 2, "cloves")
                ),
                "Pan-fry the tofu until golden.",
                "Cook broccoli with garlic and olive oil.",
                "Serve both over rice with soy sauce."
        ));
        recipes.add(recipe(
                "Chicken Mushroom Soup",
                "Comforting soup with chicken, mushroom, and milk.",
                25,
                ingredients(
                        item("Chicken Breast", 180, "g"),
                        item("Mushroom", 100, "g"),
                        item("Milk", 180, "ml"),
                        item("Onion", 0.5, "pc"),
                        item("Butter", 1, "tbsp"),
                        item("Black Pepper", 1, "tsp")
                ),
                "Cook onion and mushroom in butter until fragrant.",
                "Add chicken and cook until opaque.",
                "Pour in milk, simmer gently, and season."
        ));
        recipes.add(recipe(
                "Potato Egg Salad",
                "Creamy-style potato salad without heavy dressing.",
                20,
                ingredients(
                        item("Potato", 2, "pcs"),
                        item("Egg", 2, "pcs"),
                        item("Cucumber", 0.5, "pc"),
                        item("Yogurt", 80, "g"),
                        item("Black Pepper", 0.5, "tsp")
                ),
                "Boil the potatoes and eggs until cooked.",
                "Dice the cucumber and chop the eggs.",
                "Mix everything with yogurt and black pepper."
        ));
        recipes.add(recipe(
                "Cheesy Tomato Toast",
                "Simple toast for breakfast or snack time.",
                10,
                ingredients(
                        item("Bread", 2, "slices"),
                        item("Tomato", 1, "pc"),
                        item("Cheddar Cheese", 40, "g"),
                        item("Butter", 1, "tsp"),
                        item("Black Pepper", 0.5, "tsp")
                ),
                "Butter the bread lightly.",
                "Top with sliced tomato and cheese.",
                "Toast until golden and finish with black pepper."
        ));
        recipes.add(recipe(
                "Salmon Spinach Pasta",
                "Creamy pasta with salmon and wilted spinach.",
                25,
                ingredients(
                        item("Noodles", 180, "g"),
                        item("Salmon", 160, "g"),
                        item("Spinach", 80, "g"),
                        item("Milk", 150, "ml"),
                        item("Garlic", 2, "cloves"),
                        item("Black Pepper", 1, "tsp")
                ),
                "Cook the noodles and set aside.",
                "Pan-sear salmon with garlic, then flake it.",
                "Add milk and spinach, then toss with noodles."
        ));
        recipes.add(recipe(
                "Beef Stuffed Bell Pepper",
                "Bell peppers filled with beef and rice.",
                35,
                ingredients(
                        item("Beef", 220, "g"),
                        item("Bell Pepper", 2, "pcs"),
                        item("Rice", 1, "bowl"),
                        item("Onion", 0.5, "pc"),
                        item("Garlic", 2, "cloves"),
                        item("Black Pepper", 1, "tsp")
                ),
                "Cut the bell peppers in half and remove the seeds.",
                "Cook beef, onion, and garlic, then mix with rice.",
                "Fill the peppers and bake until tender."
        ));
        recipes.add(recipe(
                "Lemon Yogurt Fruit Bowl",
                "A light snack with fruit and tangy yogurt.",
                5,
                ingredients(
                        item("Apple", 1, "pc"),
                        item("Banana", 1, "pc"),
                        item("Yogurt", 120, "g"),
                        item("Lemon", 0.25, "pc")
                ),
                "Dice the apple and slice the banana.",
                "Spoon yogurt into a bowl.",
                "Top with fruit and a little lemon juice."
        ));
        recipes.add(recipe(
                "Chicken Carrot Rice Soup",
                "Mild soup that uses rice for body.",
                28,
                ingredients(
                        item("Chicken Breast", 180, "g"),
                        item("Carrot", 1, "pc"),
                        item("Rice", 0.5, "bowl"),
                        item("Onion", 0.5, "pc"),
                        item("Black Pepper", 1, "tsp")
                ),
                "Dice the chicken, carrot, and onion.",
                "Simmer them together until tender.",
                "Add rice near the end and season well."
        ));
        recipes.add(recipe(
                "Tofu Mushroom Stir-Fry",
                "Savory tofu stir-fry with mushroom and soy sauce.",
                16,
                ingredients(
                        item("Tofu", 220, "g"),
                        item("Mushroom", 120, "g"),
                        item("Bell Pepper", 1, "pc"),
                        item("Soy Sauce", 1.5, "tbsp"),
                        item("Garlic", 2, "cloves"),
                        item("Olive Oil", 1, "tbsp")
                ),
                "Pan-fry tofu until lightly crisp.",
                "Add mushroom, bell pepper, and garlic.",
                "Season with soy sauce and stir-fry briefly."
        ));
        recipes.add(recipe(
                "Broccoli Egg Fried Rice",
                "Vegetable fried rice with extra protein.",
                15,
                ingredients(
                        item("Rice", 1, "bowl"),
                        item("Egg", 2, "pcs"),
                        item("Broccoli", 100, "g"),
                        item("Garlic", 2, "cloves"),
                        item("Soy Sauce", 1, "tbsp"),
                        item("Olive Oil", 1, "tbsp")
                ),
                "Chop the broccoli into small florets.",
                "Scramble the eggs first and set aside.",
                "Stir-fry broccoli and rice, then return eggs and soy sauce."
        ));
        recipes.add(recipe(
                "Shrimp Tomato Pasta",
                "Light pasta with shrimp and fresh tomato.",
                18,
                ingredients(
                        item("Noodles", 180, "g"),
                        item("Shrimp", 180, "g"),
                        item("Tomato", 2, "pcs"),
                        item("Garlic", 2, "cloves"),
                        item("Olive Oil", 1, "tbsp"),
                        item("Black Pepper", 1, "tsp")
                ),
                "Cook the noodles and set aside.",
                "Saute shrimp, garlic, and chopped tomato.",
                "Combine with noodles and season with black pepper."
        ));
        recipes.add(recipe(
                "Chicken Cheese Sandwich",
                "Warm sandwich with chicken, cheese, and tomato.",
                12,
                ingredients(
                        item("Bread", 2, "slices"),
                        item("Chicken Breast", 120, "g"),
                        item("Cheddar Cheese", 30, "g"),
                        item("Tomato", 0.5, "pc"),
                        item("Butter", 1, "tsp"),
                        item("Black Pepper", 0.5, "tsp")
                ),
                "Cook the chicken and slice it thinly.",
                "Layer chicken, tomato, and cheese on bread.",
                "Toast in butter until crisp on both sides."
        ));
        recipes.add(recipe(
                "Beef Carrot Noodles",
                "Weeknight noodles with beef and carrot strips.",
                20,
                ingredients(
                        item("Noodles", 180, "g"),
                        item("Beef", 180, "g"),
                        item("Carrot", 1, "pc"),
                        item("Onion", 0.5, "pc"),
                        item("Soy Sauce", 1.5, "tbsp"),
                        item("Olive Oil", 1, "tbsp")
                ),
                "Cook the noodles and drain.",
                "Stir-fry beef, carrot, and onion in olive oil.",
                "Add noodles and soy sauce, then toss well."
        ));
        recipes.add(recipe(
                "Salmon Potato Hash",
                "Golden potato hash topped with flaky salmon.",
                22,
                ingredients(
                        item("Salmon", 150, "g"),
                        item("Potato", 2, "pcs"),
                        item("Onion", 0.5, "pc"),
                        item("Olive Oil", 1, "tbsp"),
                        item("Black Pepper", 1, "tsp")
                ),
                "Dice the potato and onion small.",
                "Cook them in olive oil until crisp and tender.",
                "Top with cooked salmon flakes and black pepper."
        ));
        recipes.add(recipe(
                "Spinach Yogurt Dip Toast",
                "Savory toast with a creamy spinach topping.",
                10,
                ingredients(
                        item("Bread", 2, "slices"),
                        item("Spinach", 50, "g"),
                        item("Yogurt", 80, "g"),
                        item("Garlic", 1, "clove"),
                        item("Black Pepper", 0.5, "tsp")
                ),
                "Wilt the spinach and mince the garlic.",
                "Mix spinach, yogurt, garlic, and black pepper.",
                "Spread on toasted bread and serve."
        ));
        recipes.add(recipe(
                "Tomato Tofu Rice Bowl",
                "A simple rice bowl with tofu and tomato.",
                16,
                ingredients(
                        item("Rice", 1, "bowl"),
                        item("Tofu", 180, "g"),
                        item("Tomato", 1, "pc"),
                        item("Soy Sauce", 1, "tbsp"),
                        item("Olive Oil", 1, "tbsp"),
                        item("Black Pepper", 0.5, "tsp")
                ),
                "Pan-fry tofu until golden.",
                "Cook tomato briefly with soy sauce and olive oil.",
                "Serve over rice and finish with black pepper."
        ));
        recipes.add(recipe(
                "Apple Banana Yogurt Smooth Bowl",
                "Chilled fruit bowl with yogurt and lemon.",
                5,
                ingredients(
                        item("Apple", 1, "pc"),
                        item("Banana", 1, "pc"),
                        item("Yogurt", 150, "g"),
                        item("Lemon", 0.25, "pc")
                ),
                "Slice the fruit into small pieces.",
                "Stir yogurt with a little lemon juice.",
                "Top the yogurt with apple and banana."
        ));
        recipes.add(recipe(
                "Mushroom Onion Omelette",
                "Soft omelette with mushroom and onion.",
                12,
                ingredients(
                        item("Egg", 3, "pcs"),
                        item("Mushroom", 80, "g"),
                        item("Onion", 0.5, "pc"),
                        item("Butter", 1, "tsp"),
                        item("Black Pepper", 0.5, "tsp")
                ),
                "Saute mushroom and onion in butter.",
                "Whisk the eggs with black pepper.",
                "Pour eggs into the pan and fold around the filling."
        ));
        return recipes;
    }

    /**
     * Creates a recipe object with its metadata, ingredient quantities, cooking steps, and image name.
     */
    private static Recipe recipe(
            String name,
            String description,
            int cookingTime,
            Map<String, Quantity> ingredients,
            String... steps) {
        return new Recipe(
                name,
                description,
                Arrays.asList(steps),
                ingredients,
                cookingTime,
                false,
                buildImageResourceName(name)
        );
    }

    /**
     * Converts a recipe title into the matching drawable resource name.
     */
    private static String buildImageResourceName(String recipeName) {
        String normalizedName = recipeName == null ? "" : recipeName.trim().toLowerCase();
        return "recipe_" + normalizedName
                .replace("&", "and")
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
    }

    /**
     * Builds an ordered ingredient quantity map for a recipe.
     */
    private static Map<String, Quantity> ingredients(IngredientQuantity... items) {
        Map<String, Quantity> ingredientMap = new LinkedHashMap<>();
        for (IngredientQuantity item : items) {
            ingredientMap.put(item.name, new Quantity(item.amount, item.unit));
        }
        return ingredientMap;
    }

    /**
     * Creates a temporary ingredient quantity entry before it is added to a recipe map.
     */
    private static IngredientQuantity item(String name, double amount, String unit) {
        return new IngredientQuantity(name, amount, unit);
    }

    private static final class IngredientQuantity {
        private final String name;
        private final double amount;
        private final String unit;

        private IngredientQuantity(String name, double amount, String unit) {
            this.name = name;
            this.amount = amount;
            this.unit = unit;
        }
    }
}
