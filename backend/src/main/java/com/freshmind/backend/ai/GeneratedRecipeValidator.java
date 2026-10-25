package com.freshmind.backend.ai;

import com.freshmind.backend.catalog.IngredientNameNormalizer;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class GeneratedRecipeValidator {

    private static final BigDecimal MAX_AMOUNT = new BigDecimal("1000000");

    public List<GeneratedRecipe> validate(
            List<GeneratedRecipe> candidates,
            Map<String, String> canonicalNames) {
        if (candidates == null || candidates.isEmpty()) {
            return List.of();
        }
        List<GeneratedRecipe> valid = new ArrayList<>();
        for (GeneratedRecipe candidate : candidates.stream().limit(30).toList()) {
            GeneratedRecipe sanitized = validateOne(candidate, canonicalNames);
            if (sanitized != null) {
                valid.add(sanitized);
            }
        }
        return List.copyOf(valid);
    }

    private GeneratedRecipe validateOne(
            GeneratedRecipe candidate,
            Map<String, String> canonicalNames) {
        if (candidate == null
                || !validText(candidate.name(), 120)
                || !validText(candidate.description(), 500)
                || candidate.cookingMinutes() < 1
                || candidate.cookingMinutes() > 180
                || candidate.ingredients() == null
                || candidate.ingredients().size() < 2
                || candidate.ingredients().size() > 12
                || candidate.steps() == null
                || candidate.steps().size() < 2
                || candidate.steps().size() > 10) {
            return null;
        }

        Set<String> distinctNames = new HashSet<>();
        List<GeneratedIngredient> ingredients = new ArrayList<>();
        for (GeneratedIngredient ingredient : candidate.ingredients()) {
            if (ingredient == null
                    || !validText(ingredient.name(), 100)
                    || ingredient.amount() == null
                    || ingredient.amount().signum() <= 0
                    || ingredient.amount().compareTo(MAX_AMOUNT) > 0
                    || !validText(ingredient.unit(), 24)) {
                return null;
            }
            String normalized = IngredientNameNormalizer.normalize(ingredient.name());
            String canonicalName = canonicalNames.get(normalized);
            if (canonicalName == null || !distinctNames.add(normalized)) {
                return null;
            }
            ingredients.add(new GeneratedIngredient(
                    canonicalName,
                    ingredient.amount().stripTrailingZeros(),
                    ingredient.unit().strip()));
        }

        List<String> steps = new ArrayList<>();
        for (String step : candidate.steps()) {
            if (!validText(step, 500)) {
                return null;
            }
            steps.add(step.strip());
        }

        return new GeneratedRecipe(
                candidate.name().strip(),
                candidate.description().strip(),
                candidate.cookingMinutes(),
                List.copyOf(ingredients),
                List.copyOf(steps));
    }

    private boolean validText(String value, int maximumLength) {
        return value != null && !value.isBlank() && value.strip().length() <= maximumLength;
    }
}
