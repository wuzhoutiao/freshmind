package com.example.freshmind;

import com.example.freshmind.model.InventoryItem;

import java.util.ArrayList;
import java.util.List;

/**
 * Test-only inventory data that simulates records loaded from storage.
 * Keep this separate from InventoryDatabase so production catalog data stays unchanged.
 */
final class TestInventoryStorageData {

    static final String BASE_TODAY = "2026-05-18";

    private TestInventoryStorageData() {
    }

    static InventoryServiceSeed createSeed() {
        return new InventoryServiceSeed(createStorageItems());
    }

    static List<InventoryItem> createStorageItems() {
        List<InventoryItem> items = new ArrayList<>();

        items.add(item(1, "Egg", "Others", "6 pcs", "2026-05-01", "2026-05-19"));
        items.add(item(2, "Tomato", "Vegetables", "4 pcs", "2026-05-16", "2026-05-20"));
        items.add(item(3, "Chicken Breast", "Meat", "500 g", "2026-05-18", "2026-05-19"));
        items.add(item(4, "Salmon", "Meat", "2 fillets", "2026-05-18", "2026-05-19"));
        items.add(item(5, "Milk", "Others", "1 bottle", "2026-05-14", "2026-05-20"));
        items.add(item(6, "Shrimp", "Meat", "300 g", "2026-05-18", "2026-05-20"));
        items.add(item(7, "Lettuce", "Vegetables", "1 head", "2026-05-16", "2026-05-20"));
        items.add(item(8, "Garlic", "Vegetables", "1 bulb", "2026-05-01", "2026-05-21"));
        items.add(item(9, "Beef", "Meat", "400 g", "2026-05-18", "2026-05-21"));
        items.add(item(10, "Mushroom", "Vegetables", "250 g", "2026-05-18", "2026-05-22"));
        items.add(item(11, "Tofu", "Others", "1 block", "2026-05-18", "2026-05-23"));
        items.add(item(12, "Kale", "Vegetables", "1 bunch", "2026-05-18", "2026-05-24"));
        items.add(item(13, "Broccoli", "Vegetables", "2 heads", "2026-05-18", "2026-05-25"));
        items.add(item(14, "Banana", "Fruits", "5 pcs", "2026-05-18", "2026-05-25"));
        items.add(item(15, "Greek Yogurt", "Others", "500 g", "2026-05-18", "2026-05-29"));
        items.add(item(16, "Tempeh", "Others", "250 g", "2026-05-18", "2026-05-30"));
        items.add(item(17, "Lemon", "Fruits", "3 pcs", "2026-05-18", "2026-06-01"));
        items.add(item(18, "Cheddar Cheese", "Others", "200 g", "2026-05-18", "2026-06-08"));
        items.add(item(19, "Potato", "Vegetables", "1 kg", "2026-05-18", "2026-06-17"));
        items.add(item(20, "Onion", "Vegetables", "6 pcs", "2026-05-18", "2026-06-17"));
        items.add(item(21, "Apple", "Fruits", "6 pcs", "2026-05-18", "2026-06-17"));
        items.add(item(22, "Blueberry", "Fruits", "2 punnets", "2026-05-18", "2026-06-30"));
        items.add(item(23, "Kimchi", "Others", "1 jar", "2026-05-18", "2026-08-18"));
        items.add(item(24, "Noodles", "Others", "500 g", "2026-05-18", "2026-11-14"));
        items.add(item(25, "Frozen Peas", "Vegetables", "1 bag", "2026-05-18", "2026-12-18"));
        items.add(item(26, "Quinoa", "Others", "750 g", "2026-05-18", "2027-02-12"));
        items.add(item(27, "Soy Sauce", "Others", "1 bottle", "2026-05-18", "2027-05-18"));
        items.add(item(28, "Coconut Milk", "Others", "2 cans", "2026-05-18", "2027-11-18"));
        items.add(item(29, "Rice", "Others", "5 kg", "2026-05-18", "2028-05-18"));
        items.add(item(30, "Black Pepper", "Others", "1 grinder", "2026-05-18", "2028-05-18"));
        items.add(item(31, "Avocado", "Fruits", "2 pcs", "2026-05-12", "2026-05-17"));
        items.add(item(32, "Pork", "Meat", "350 g", "2026-05-13", "2026-05-15"));

        return items;
    }

    private static InventoryItem item(int id, String name, String category, String quantity,
                                      String purchaseDate, String expiryDate) {
        return new InventoryItem(id, name, category, quantity, purchaseDate, expiryDate);
    }

    static final class InventoryServiceSeed {
        private final List<InventoryItem> items;

        private InventoryServiceSeed(List<InventoryItem> items) {
            this.items = items;
        }

        List<InventoryItem> getItems() {
            return new ArrayList<>(items);
        }
    }
}
