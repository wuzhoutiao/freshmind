package com.example.freshmind;

import android.content.Context;

import com.example.freshmind.data.InventoryStorage;
import com.example.freshmind.model.InventoryItem;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

final class TestInventoryStorageSeeder {

    private static final SimpleDateFormat DATE_FORMAT =
            new SimpleDateFormat("yyyy-MM-dd", Locale.US);

    private TestInventoryStorageSeeder() {
    }

    static void seedThirtyStorageItems(Context context) {
        InventoryStorage storage = new InventoryStorage(context);
        storage.saveItems(createStorageItems());
    }

    static void clear(Context context) {
        new InventoryStorage(context).clearItems();
    }

    private static List<InventoryItem> createStorageItems() {
        List<InventoryItem> items = new ArrayList<>();

        items.add(item(1, "Egg", "Others", "6 pcs", -17, 1));
        items.add(item(2, "Tomato", "Vegetables", "4 pcs", -2, 2));
        items.add(item(3, "Chicken Breast", "Meat", "500 g", 0, 1));
        items.add(item(4, "Salmon", "Meat", "2 fillets", 0, 1));
        items.add(item(5, "Milk", "Others", "1 bottle", -4, 2));
        items.add(item(6, "Shrimp", "Meat", "300 g", 0, 2));
        items.add(item(7, "Lettuce", "Vegetables", "1 head", -2, 2));
        items.add(item(8, "Garlic", "Vegetables", "1 bulb", -17, 3));
        items.add(item(9, "Beef", "Meat", "400 g", 0, 3));
        items.add(item(10, "Mushroom", "Vegetables", "250 g", 0, 4));
        items.add(item(11, "Tofu", "Others", "1 block", 0, 5));
        items.add(item(12, "Kale", "Vegetables", "1 bunch", 0, 6));
        items.add(item(13, "Broccoli", "Vegetables", "2 heads", 0, 7));
        items.add(item(14, "Banana", "Fruits", "5 pcs", 0, 7));
        items.add(item(15, "Greek Yogurt", "Others", "500 g", 0, 11));
        items.add(item(16, "Tempeh", "Others", "250 g", 0, 12));
        items.add(item(17, "Lemon", "Fruits", "3 pcs", 0, 14));
        items.add(item(18, "Cheddar Cheese", "Others", "200 g", 0, 21));
        items.add(item(19, "Potato", "Vegetables", "1 kg", 0, 30));
        items.add(item(20, "Onion", "Vegetables", "6 pcs", 0, 30));
        items.add(item(21, "Apple", "Fruits", "6 pcs", 0, 30));
        items.add(item(22, "Blueberry", "Fruits", "2 punnets", 0, 43));
        items.add(item(23, "Kimchi", "Others", "1 jar", 0, 92));
        items.add(item(24, "Noodles", "Others", "500 g", 0, 180));
        items.add(item(25, "Frozen Peas", "Vegetables", "1 bag", 0, 214));
        items.add(item(26, "Quinoa", "Others", "750 g", 0, 270));
        items.add(item(27, "Soy Sauce", "Others", "1 bottle", 0, 365));
        items.add(item(28, "Coconut Milk", "Others", "2 cans", 0, 549));
        items.add(item(29, "Rice", "Others", "5 kg", 0, 730));
        items.add(item(30, "Black Pepper", "Others", "1 grinder", 0, 730));
        items.add(item(31, "Avocado", "Fruits", "2 pcs", -6, -1));
        items.add(item(32, "Pork", "Meat", "350 g", -5, -3));

        return items;
    }

    private static InventoryItem item(int id, String name, String category, String quantity,
                                      int purchaseOffsetDays, int expiryOffsetDays) {
        return new InventoryItem(
                id,
                name,
                category,
                quantity,
                dateFromToday(purchaseOffsetDays),
                dateFromToday(expiryOffsetDays)
        );
    }

    private static String dateFromToday(int offsetDays) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, offsetDays);
        return DATE_FORMAT.format(calendar.getTime());
    }
}
