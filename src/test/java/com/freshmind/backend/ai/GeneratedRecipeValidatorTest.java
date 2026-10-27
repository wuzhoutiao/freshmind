package com.freshmind.backend.ai;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class GeneratedRecipeValidatorTest {

    private final GeneratedRecipeValidator validator = new GeneratedRecipeValidator();
    private final Map<String, String> catalog = Map.of(
            "tomato", "Tomato",
            "egg", "Egg",
            "rice", "Rice");

    @Test
    void validCandidateIsTrimmedAndUsesCanonicalCatalogNames() {
        GeneratedRecipe candidate = recipe(
                "  Tomato Egg  ",
                10,
                List.of(
                        ingredient(" tomato ", "2"),
                        ingredient("EGG", "1")),
                List.of("  Prepare the ingredients. ", "Cook thoroughly."));

        List<GeneratedRecipe> result = validator.validate(List.of(candidate), catalog);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("Tomato Egg");
        assertThat(result.get(0).ingredients())
                .extracting(GeneratedIngredient::name)
                .containsExactly("Tomato", "Egg");
        assertThat(result.get(0).steps().get(0)).isEqualTo("Prepare the ingredients.");
    }

    @Test
    void rejectsWholeCandidateWhenAnyModelFieldViolatesRules() {
        GeneratedRecipe unknownIngredient = recipe(
                "Unknown",
                10,
                List.of(ingredient("Tomato", "1"), ingredient("Dragon Fruit", "1")),
                validSteps());
        GeneratedRecipe duplicateIngredient = recipe(
                "Duplicate",
                10,
                List.of(ingredient("Tomato", "1"), ingredient(" tomato ", "2")),
                validSteps());
        GeneratedRecipe invalidAmount = recipe(
                "Amount",
                10,
                List.of(ingredient("Tomato", "0"), ingredient("Egg", "1")),
                validSteps());
        GeneratedRecipe emptyStep = recipe(
                "Steps",
                10,
                List.of(ingredient("Tomato", "1"), ingredient("Egg", "1")),
                List.of("Prepare.", " "));
        GeneratedRecipe invalidMinutes = recipe(
                "Minutes",
                181,
                List.of(ingredient("Tomato", "1"), ingredient("Egg", "1")),
                validSteps());

        assertThat(validator.validate(List.of(
                unknownIngredient,
                duplicateIngredient,
                invalidAmount,
                emptyStep,
                invalidMinutes), catalog)).isEmpty();
    }

    private GeneratedRecipe recipe(
            String name,
            int minutes,
            List<GeneratedIngredient> ingredients,
            List<String> steps) {
        return new GeneratedRecipe(name, "A valid description.", minutes, ingredients, steps);
    }

    private GeneratedIngredient ingredient(String name, String amount) {
        return new GeneratedIngredient(name, new BigDecimal(amount), "pcs");
    }

    private List<String> validSteps() {
        return List.of("Prepare.", "Cook thoroughly.");
    }
}
