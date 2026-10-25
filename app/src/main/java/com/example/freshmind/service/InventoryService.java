package com.example.freshmind.service;

import com.example.freshmind.model.InventoryItem;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Handles basic create, read, update, and delete operations for user inventory items.
 * This class manages user-owned food records, not the static ingredient catalog.
 */
public class InventoryService {

    /**
     * Stores the user's current inventory records in memory.
     * Each element in this list is one InventoryItem added by the user.
     */
    private final List<InventoryItem> inventoryItems;

    /**
     * Creates an empty inventory service.
     * Use this when the app starts with no user inventory records loaded yet.
     */
    public InventoryService() {
        this.inventoryItems = new ArrayList<>();
    }

    /**
     * Creates an inventory service with existing inventory records.
     * This is useful for loading sample data or previously saved user data.
     */
    public InventoryService(List<InventoryItem> initialItems) {
        this();
        if (initialItems == null) {
            return;
        }
        for (InventoryItem item : initialItems) {
            addItem(item);
        }
    }

    /**
     * Creates a new inventory item if its id is not already used.
     * Returns true when the item is added, and false when another item already has the same id.
     */
    public boolean addItem(InventoryItem item) {
        validateItem(item);
        if (findItemById(item.getId()) != null) {
            return false;
        }
        inventoryItems.add(item);
        return true;
    }

    /**
     * Reads all inventory items as a copy, so outside code cannot edit the internal list directly.
     * The caller can display or sort the returned list without changing this service's stored data.
     */
    public List<InventoryItem> getAllItems() {
        return new ArrayList<>(inventoryItems);
    }

    /**
     * Finds one inventory item by its unique id.
     * Returns null if no item with that id exists.
     */
    public InventoryItem findItemById(int id) {
        for (InventoryItem item : inventoryItems) {
            if (item.getId() == id) {
                return item;
            }
        }
        return null;
    }

    /**
     * Finds all inventory records with the same ingredient name.
     * The comparison ignores uppercase/lowercase differences and extra spaces.
     */
    public List<InventoryItem> findItemsByName(String ingredientName) {
        List<InventoryItem> results = new ArrayList<>();
        String targetName = normalize(ingredientName);

        for (InventoryItem item : inventoryItems) {
            if (normalize(item.getIngredientName()).equals(targetName)) {
                results.add(item);
            }
        }

        return results;
    }

    /**
     * Finds all inventory records in one category.
     * This can support category filters on the inventory page.
     */
    public List<InventoryItem> getItemsByCategory(String category) {
        List<InventoryItem> results = new ArrayList<>();
        String targetCategory = normalize(category);

        for (InventoryItem item : inventoryItems) {
            if (normalize(item.getCategory()).equals(targetCategory)) {
                results.add(item);
            }
        }

        return results;
    }

    /**
     * Updates an existing item by matching its id.
     * Returns true when the item is found and replaced, and false when the id does not exist.
     */
    public boolean updateItem(InventoryItem updatedItem) {
        validateItem(updatedItem);

        for (int i = 0; i < inventoryItems.size(); i++) {
            if (inventoryItems.get(i).getId() == updatedItem.getId()) {
                inventoryItems.set(i, updatedItem);
                return true;
            }
        }

        return false;
    }

    /**
     * Deletes one inventory item by its id.
     * Returns true when an item is removed, and false when the id is not found.
     */
    public boolean removeItemById(int id) {
        for (int i = 0; i < inventoryItems.size(); i++) {
            if (inventoryItems.get(i).getId() == id) {
                inventoryItems.remove(i);
                return true;
            }
        }
        return false;
    }

    /**
     * Returns a copy of the inventory sorted by expiry date from earliest to latest.
     * This helps show the most urgent food items first.
     */
    public List<InventoryItem> getItemsSortedByExpiryDate() {
        List<InventoryItem> sortedItems = getAllItems();
        sortedItems.sort(Comparator.comparing(InventoryItem::getExpiryDate));
        return sortedItems;
    }

    /**
     * Finds the next id that can be used for a new inventory item.
     * It checks the current largest id and returns largest id + 1.
     */
    public int getNextAvailableId() {
        int maxId = 0;
        for (InventoryItem item : inventoryItems) {
            if (item.getId() > maxId) {
                maxId = item.getId();
            }
        }
        return maxId + 1;
    }

    /**
     * Returns how many inventory records are currently stored.
     * This is useful for simple dashboard counts or tests.
     */
    public int getItemCount() {
        return inventoryItems.size();
    }

    /**
     * Checks that the caller did not pass a null InventoryItem.
     * This avoids unclear errors later in the CRUD methods.
     */
    private void validateItem(InventoryItem item) {
        if (item == null) {
            throw new IllegalArgumentException("Inventory item cannot be null.");
        }
    }

    /**
     * Converts text to a standard comparison format.
     * For example, " Egg " and "egg" both become "egg".
     */
    private String normalize(String text) {
        if (text == null) {
            return "";
        }
        return text.trim().toLowerCase();
    }
}
