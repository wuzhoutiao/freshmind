package com.freshmind.backend.ai;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile({"test", "local"})
public class DemoRecipeGenerationClient implements RecipeGenerationClient {

    @Override
    public List<GeneratedRecipe> generate(
            List<PantryIngredient> pantry,
            List<String> allowedIngredients) {
        return List.of(
                recipe(
                        "Tomato Egg Fried Rice",
                        "A quick pantry fried rice that prioritizes tomato and egg.",
                        15,
                        List.of(
                                ingredient("Rice", "1", "bowl"),
                                ingredient("Egg", "2", "pcs"),
                                ingredient("Tomato", "2", "pcs"),
                                ingredient("Soy Sauce", "1", "tbsp"),
                                ingredient("Olive Oil", "1", "tbsp")),
                        "Scramble the eggs and set aside.",
                        "Cook the tomato until juicy.",
                        "Add rice, egg and soy sauce, then toss until hot."),
                recipe(
                        "Chicken Broccoli Stir Fry",
                        "A simple stir fry using common refrigerator ingredients.",
                        20,
                        List.of(
                                ingredient("Chicken Breast", "200", "g"),
                                ingredient("Broccoli", "150", "g"),
                                ingredient("Garlic", "2", "cloves"),
                                ingredient("Soy Sauce", "1", "tbsp")),
                        "Slice the chicken and broccoli.",
                        "Cook the chicken completely with garlic.",
                        "Add broccoli and soy sauce, then stir fry until tender."),
                recipe(
                        "Apple Banana Yogurt Bowl",
                        "A fast chilled fruit snack.",
                        5,
                        List.of(
                                ingredient("Apple", "1", "pc"),
                                ingredient("Banana", "1", "pc"),
                                ingredient("Yogurt", "150", "g"),
                                ingredient("Lemon", "0.25", "pc")),
                        "Dice the apple and banana.",
                        "Stir the yogurt with lemon.",
                        "Top the yogurt with fruit and serve."));
    }

    private GeneratedRecipe recipe(
            String name,
            String description,
            int minutes,
            List<GeneratedIngredient> ingredients,
            String... steps) {
        return new GeneratedRecipe(name, description, minutes, ingredients, List.of(steps));
    }

    private GeneratedIngredient ingredient(String name, String amount, String unit) {
        return new GeneratedIngredient(name, new BigDecimal(amount), unit);
    }
}
