package com.example.freshmind.model.recipe;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a recipe with ingredients, cooking steps, timing, favorite state, and image metadata.
 */
public class Recipe {
    private static int nextId = 1;

    private final int id;
    private String name;
    private String description;
    private final List<String> steps;
    private final Map<String, Quantity> ingredientsNeeded;
    private Integer cookingTime;
    private boolean favorite;
    private String imageResName;

    /**
     * Creates a recipe without an image resource name.
     */
    public Recipe(
            String name,
            String description,
            List<String> steps,
            Map<String, Quantity> ingredientsNeeded,
            Integer cookingTime,
            boolean favorite) {
        this(name, description, steps, ingredientsNeeded, cookingTime, favorite, "");
    }

    /**
     * Creates a recipe with full display and cooking information.
     */
    public Recipe(
            String name,
            String description,
            List<String> steps,
            Map<String, Quantity> ingredientsNeeded,
            Integer cookingTime,
            boolean favorite,
            String imageResName) {
        this.id = nextId++;
        this.name = validateText(name, "Recipe name");
        this.description = description == null ? "" : description.trim();
        this.steps = new ArrayList<>();
        this.ingredientsNeeded = new LinkedHashMap<>();
        this.cookingTime = validateNonNegative(cookingTime);
        this.favorite = favorite;
        this.imageResName = normalizeOptionalText(imageResName);
        setSteps(steps);
        setIngredientsNeeded(ingredientsNeeded);
    }

    /**
     * Creates a recipe with only a name and default empty details.
     */
    public Recipe(String name) {
        this(name, "", null, null, null, false);
    }

    /**
     * Recreates a recipe with a fixed ID, usually when importing saved data.
     */
    private Recipe(
            int id,
            String name,
            String description,
            List<String> steps,
            Map<String, Quantity> ingredientsNeeded,
            Integer cookingTime,
            boolean favorite) {
        this(id, name, description, steps, ingredientsNeeded, cookingTime, favorite, "");
    }

    private Recipe(
            int id,
            String name,
            String description,
            List<String> steps,
            Map<String, Quantity> ingredientsNeeded,
            Integer cookingTime,
            boolean favorite,
            String imageResName) {

        this.id = id;

        nextId = Math.max(nextId, id + 1);

        this.name = validateText(name, "Recipe name");
        this.description = description == null ? "" : description.trim();
        this.steps = new ArrayList<>();
        this.ingredientsNeeded = new LinkedHashMap<>();
        this.cookingTime = validateNonNegative(cookingTime);
        this.favorite = favorite;
        this.imageResName = normalizeOptionalText(imageResName);

        setSteps(steps);
        setIngredientsNeeded(ingredientsNeeded);
    }

    /**
     * Builds a recipe from imported data without image metadata.
     */
    public static Recipe fromImportedData(
            int id,
            String name,
            String description,
            List<String> steps,
            Map<String, Quantity> ingredientsNeeded,
            Integer cookingTime,
            boolean favorite) {

        return new Recipe(id, name, description, steps, ingredientsNeeded, cookingTime, favorite);
    }

    /**
     * Builds a recipe from imported data with image metadata.
     */
    public static Recipe fromImportedData(
            int id,
            String name,
            String description,
            List<String> steps,
            Map<String, Quantity> ingredientsNeeded,
            Integer cookingTime,
            boolean favorite,
            String imageResName) {

        return new Recipe(id, name, description, steps, ingredientsNeeded, cookingTime, favorite, imageResName);
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = validateText(name, "Recipe name");
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description == null ? "" : description.trim();
    }

    public Integer getCookingTime() {
        return cookingTime;
    }

    public void setCookingTime(Integer cookingTime) {
        this.cookingTime = validateNonNegative(cookingTime);
    }

    public String getCookingTimeDisplay() { return cookingTime == null ? "Not included" : cookingTime + " minute"; }

    public boolean isFavorite() {
        return favorite;
    }

    public void addFavorite() {
        favorite = true;
    }

    public void removeFavorite() {
        favorite = false;
    }

    public void setFavorite(boolean favorite) {
        this.favorite = favorite;
    }

    public String getImageResName() {
        return imageResName;
    }

    public void setImageResName(String imageResName) {
        this.imageResName = normalizeOptionalText(imageResName);
    }

    /**
     * Adds one validated cooking step to the recipe.
     */
    public void addStep(String step) {
        steps.add(validateText(step, "Step"));
    }

    /**
     * Adds multiple cooking steps while ignoring a null collection.
     */
    public void addSteps(Collection<String> steps) {
        if (steps == null) {
            return;
        }
        for (String step : steps) {
            addStep(step);
        }
    }

    /**
     * Replaces all cooking steps with the given list.
     */
    public void setSteps(List<String> steps) {
        this.steps.clear();
        addSteps(steps);
    }

    public void removeStep(int index) {
        steps.remove(index);
    }

    public List<String> getSteps() {
        return Collections.unmodifiableList(steps);
    }

    /**
     * Adds or replaces an ingredient using an amount and unit.
     */
    public void addIngredient(String ingredientName, double amount, String unit) {
        String key = validateText(ingredientName, "Ingredient name");
        ingredientsNeeded.put(key, new Quantity(amount, unit));
    }

    /**
     * Adds or replaces an ingredient using a prepared quantity object.
     */
    public void addIngredient(String ingredientName, Quantity quantity) {
        String key = validateText(ingredientName, "Ingredient name");
        if (quantity == null) {
            throw new IllegalArgumentException("Quantity cannot be null.");
        }
        ingredientsNeeded.put(key, quantity);
    }

    /**
     * Adds multiple required ingredients while ignoring a null map.
     */
    public void addIngredients(Map<String, Quantity> ingredientsNeeded) {
        if (ingredientsNeeded == null) {
            return;
        }
        for (Map.Entry<String, Quantity> entry : ingredientsNeeded.entrySet()) {
            addIngredient(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Replaces the full required ingredient map.
     */
    public void setIngredientsNeeded(Map<String, Quantity> ingredientsNeeded) {
        this.ingredientsNeeded.clear();
        addIngredients(ingredientsNeeded);
    }

    public void removeIngredient(String ingredientName) {
        if (ingredientName != null) {
            ingredientsNeeded.remove(ingredientName.trim());
        }
    }

    public Map<String, Quantity> getIngredientsNeeded() {
        return Collections.unmodifiableMap(ingredientsNeeded);
    }

    /**
     * Validates required text fields and returns the trimmed value.
     */
    private String validateText(String text, String fieldName) {
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " cannot be empty.");
        }
        return text.trim();
    }

    /**
     * Normalizes optional text fields while allowing empty values.
     */
    private String normalizeOptionalText(String text) {
        if (text == null) {
            return "";
        }
        return text.trim();
    }

    /**
     * Validates cooking time while allowing a null value for unknown time.
     */
    private Integer validateNonNegative(Integer value) {
        if (value == null) {
            return null;
        }
        if (value < 0) {
            throw new IllegalArgumentException("Cooking time cannot be negative.");
        }
        return value;
    }

    @NonNull
    @Override
    public String toString() {
        return "Recipe{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", cookingTime=" + getCookingTimeDisplay() +
                ", imageResName='" + imageResName + '\'' +
                ", favorite=" + favorite +
                '}';
    }
}
