package com.example.freshmind.data;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.freshmind.model.InventoryItem;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Saves and loads user inventory data with SharedPreferences.
 */
public class InventoryStorage {
    private static final String PREF_NAME = "freshmind_inventory";
    private static final String KEY_ITEMS = "inventory_items_json";

    private static final String FIELD_ID = "id";
    private static final String FIELD_INGREDIENT_NAME = "ingredientName";
    private static final String FIELD_CATEGORY = "category";
    private static final String FIELD_QUANTITY = "quantity";
    private static final String FIELD_PURCHASE_DATE = "purchaseDate";
    private static final String FIELD_EXPIRY_DATE = "expiryDate";
    private static final String FIELD_STATUS = "status";

    private static final String CATEGORY_VEGETABLES = "Vegetables";
    private static final String CATEGORY_FRUITS = "Fruits";
    private static final String CATEGORY_MEAT = "Meat";
    private static final String CATEGORY_OTHERS = "Others";

    private final SharedPreferences preferences;

    public InventoryStorage(Context context) {
        Context appContext = context.getApplicationContext();
        preferences = appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
}

    /**
     * Stores the whole inventory list as one JSON array.
     * This keeps the storage layer simple for a small coursework app.
     */
    public void saveItems(List<InventoryItem> items) {
        JSONArray jsonItems = new JSONArray();

        if (items != null) {
            for (InventoryItem item : items) {
                item.setCategory(normalizeCategory(item.getIngredientName(), item.getCategory()));
                jsonItems.put(toJson(item));
            }
        }

        preferences.edit()
                .putString(KEY_ITEMS, jsonItems.toString())
                .apply();
    }

    /**
     * Loads saved inventory records from JSON.
     * If the saved JSON is missing or broken, the app starts with an empty list.
     */
    public List<InventoryItem> loadItems() {
        String storedJson = preferences.getString(KEY_ITEMS, "[]");
        List<InventoryItem> items = new ArrayList<>();

        try {
            JSONArray jsonItems = new JSONArray(storedJson);
            for (int i = 0; i < jsonItems.length(); i++) {
                items.add(fromJson(jsonItems.getJSONObject(i)));
            }
        } catch (JSONException | IllegalArgumentException e) {
            return new ArrayList<>();
        }

        return items;
    }

    /**
     * Clears all saved inventory data. This is useful for reset or test flows.
     */
    public void clearItems() {
        preferences.edit()
                .remove(KEY_ITEMS)
                .apply();
    }

    private JSONObject toJson(InventoryItem item) {
        JSONObject jsonItem = new JSONObject();

        try {
            jsonItem.put(FIELD_ID, item.getId());
            jsonItem.put(FIELD_INGREDIENT_NAME, item.getIngredientName());
            jsonItem.put(FIELD_CATEGORY, item.getCategory());
            jsonItem.put(FIELD_QUANTITY, item.getQuantity());
            jsonItem.put(FIELD_PURCHASE_DATE, item.getPurchaseDate());
            jsonItem.put(FIELD_EXPIRY_DATE, item.getExpiryDate());
            jsonItem.put(FIELD_STATUS, item.getStatus());
        } catch (JSONException e) {
            throw new IllegalStateException("Failed to convert inventory item to JSON.", e);
        }

        return jsonItem;
    }

    private InventoryItem fromJson(JSONObject jsonItem) throws JSONException {
        return new InventoryItem(
                jsonItem.getInt(FIELD_ID),
                jsonItem.getString(FIELD_INGREDIENT_NAME),
                normalizeCategory(
                        jsonItem.getString(FIELD_INGREDIENT_NAME),
                        jsonItem.getString(FIELD_CATEGORY)
                ),
                jsonItem.getString(FIELD_QUANTITY),
                jsonItem.getString(FIELD_PURCHASE_DATE),
                jsonItem.getString(FIELD_EXPIRY_DATE),
                jsonItem.getString(FIELD_STATUS)
        );
    }

    private String normalizeCategory(String ingredientName, String category) {
        String normalizedName = normalize(ingredientName);
        if ("egg".equals(normalizedName)
                || "tofu".equals(normalizedName)
                || "tempeh".equals(normalizedName)
                || "kimchi".equals(normalizedName)) {
            return CATEGORY_OTHERS;
        }
        if ("chicken breast".equals(normalizedName)
                || "beef".equals(normalizedName)
                || "salmon".equals(normalizedName)
                || "shrimp".equals(normalizedName)
                || "pork".equals(normalizedName)) {
            return CATEGORY_MEAT;
        }
        if (CATEGORY_VEGETABLES.equals(category)) {
            return CATEGORY_VEGETABLES;
        }
        if (CATEGORY_FRUITS.equals(category)) {
            return CATEGORY_FRUITS;
        }
        if (CATEGORY_MEAT.equals(category) || "Protein".equals(category)) {
            return CATEGORY_MEAT;
        }
        return CATEGORY_OTHERS;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }
}
