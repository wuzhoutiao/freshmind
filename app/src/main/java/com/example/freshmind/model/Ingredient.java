package com.example.freshmind.model;

/**
 * Represents a food ingredient with its catalog ID, display name, category, and default shelf life.
 */
public class Ingredient {

    private int ingredientId;
    private String ingredientName;
    private String category;
    private int defaultShelfLifeDays;

    /**
     * Creates an ingredient entry for the inventory catalog.
     */
    public Ingredient(int ingredientId, String ingredientName, String category, int defaultShelfLifeDays) {
        this.ingredientId = ingredientId;
        this.ingredientName = ingredientName;
        this.category = category;
        this.defaultShelfLifeDays = defaultShelfLifeDays;
    }

    // Getters for reading ingredient details.
    public int getIngredientId() {
        return ingredientId;
    }

    public String getIngredientName() {
        return ingredientName;
    }

    public String getCategory() {
        return category;
    }

    public int getDefaultShelfLifeDays() {
        return defaultShelfLifeDays;
    }

    // Setters for updating editable ingredient fields.
    public void setIngredientName(String ingredientName) {
        this.ingredientName = ingredientName;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public void setDefaultShelfLifeDays(int days) {
        this.defaultShelfLifeDays = days;
    }
}
