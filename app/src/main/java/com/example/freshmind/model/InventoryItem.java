package com.example.freshmind.model;

/**
 * Represents one food item stored in the user's inventory.
 * This class stores user-specific data, not the general ingredient database.
 */
public class InventoryItem {

    public static final String STATUS_NORMAL = "Normal";
    public static final String STATUS_NEAR_EXPIRY = "Near Expiry";
    public static final String STATUS_EXPIRED = "Expired";

    private int id;
    private String ingredientName;
    private String category;
    private String quantity;
    private String purchaseDate;
    private String expiryDate;
    private String status;

    public InventoryItem(int id, String ingredientName, String category, String quantity,
                         String purchaseDate, String expiryDate, String status) {
        this.id = id;
        this.ingredientName = validateText(ingredientName, "Ingredient name");
        this.category = validateText(category, "Category");
        this.quantity = validateText(quantity, "Quantity");
        this.purchaseDate = validateText(purchaseDate, "Purchase date");
        this.expiryDate = validateText(expiryDate, "Expiry date");
        this.status = validateText(status, "Status");
    }

    /**
     * Convenience constructor for a new item whose status has not been checked yet.
     */
    public InventoryItem(int id, String ingredientName, String category, String quantity,
                         String purchaseDate, String expiryDate) {
        this(id, ingredientName, category, quantity, purchaseDate, expiryDate, STATUS_NORMAL);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getIngredientName() {
        return ingredientName;
    }

    public void setIngredientName(String ingredientName) {
        this.ingredientName = validateText(ingredientName, "Ingredient name");
    }

    public String getCategory() {
        return category;
    }

    /**
     * Category should use one of the four food categories agreed by the team.
     */
    public void setCategory(String category) {
        this.category = validateText(category, "Category");
    }

    public String getQuantity() {
        return quantity;
    }

    public void setQuantity(String quantity) {
        this.quantity = validateText(quantity, "Quantity");
    }

    public String getPurchaseDate() {
        return purchaseDate;
    }

    /**
     * Date format suggestion: yyyy-MM-dd.
     */
    public void setPurchaseDate(String purchaseDate) {
        this.purchaseDate = validateText(purchaseDate, "Purchase date");
    }

    public String getExpiryDate() {
        return expiryDate;
    }

    /**
     * Date format suggestion: yyyy-MM-dd.
     */
    public void setExpiryDate(String expiryDate) {
        this.expiryDate = validateText(expiryDate, "Expiry date");
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = validateText(status, "Status");
    }

    private String validateText(String text, String fieldName) {
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " cannot be empty.");
        }
        return text.trim();
    }

    @Override
    public String toString() {
        return "InventoryItem{" +
                "id=" + id +
                ", ingredientName='" + ingredientName + '\'' +
                ", category='" + category + '\'' +
                ", quantity='" + quantity + '\'' +
                ", purchaseDate='" + purchaseDate + '\'' +
                ", expiryDate='" + expiryDate + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}
