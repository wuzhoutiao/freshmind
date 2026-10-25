package com.example.freshmind.service;

import com.example.freshmind.data.InventoryDatabase;
import com.example.freshmind.model.Ingredient;
import com.example.freshmind.model.InventoryItem;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Calculates expiry dates and builds reminder lists for user inventory items.
 * It uses InventoryDatabase as the static ingredient catalog for default shelf life data.
 */
public class ExpiryReminderService {
    private static final String CATEGORY_MEAT = "Meat";
    private static final String LEGACY_CATEGORY_PROTEIN = "Protein";

    /**
     * If an item expires in this many days or fewer, it is marked as near expiry.
     */
    public static final int NEAR_EXPIRY_THRESHOLD_DAYS = 2;

    /**
     * Used when the ingredient name and category cannot be found in InventoryDatabase.
     */
    public static final int DEFAULT_SHELF_LIFE_DAYS = 7;

    /**
     * All date strings in this project should follow this format.
     */
    public static final String DATE_PATTERN = "yyyy-MM-dd";

    /**
     * Creates a user inventory record and automatically fills in expiry date and status.
     * This is useful when the user provides purchase date but not expiry date.
     */
    public InventoryItem createItemWithEstimatedExpiry(int id, String ingredientName, String category,
                                                       String quantity, String purchaseDate) {
        String expiryDate = estimateExpiryDate(ingredientName, category, purchaseDate);
        InventoryItem item = new InventoryItem(id, ingredientName, category, quantity, purchaseDate, expiryDate);
        updateItemStatus(item);
        return item;
    }

    /**
     * Estimates an expiry date by adding the default shelf life to the purchase date.
     * The default shelf life comes from InventoryDatabase when possible.
     */
    public String estimateExpiryDate(String ingredientName, String category, String purchaseDate) {
        int shelfLifeDays = getDefaultShelfLifeDays(ingredientName, category);
        return addDays(purchaseDate, shelfLifeDays);
    }

    /**
     * Finds the best shelf life value for an ingredient.
     * It first matches by ingredient name, then by category average, then falls back to 7 days.
     */
    public int getDefaultShelfLifeDays(String ingredientName, String category) {
        Ingredient exactIngredient = findIngredientByName(ingredientName);
        if (exactIngredient != null) {
            return exactIngredient.getDefaultShelfLifeDays();
        }

        Integer categoryAverage = getAverageShelfLifeByCategory(category);
        if (categoryAverage != null) {
            return categoryAverage;
        }

        return DEFAULT_SHELF_LIFE_DAYS;
    }

    /**
     * Calculates the status of an item using today's real date.
     */
    public String calculateStatus(InventoryItem item) {
        return calculateStatus(item, getTodayString());
    }

    /**
     * Calculates whether an item is Normal, Near Expiry, or Expired.
     * This overloaded version accepts a custom today date, which makes testing easier.
     */
    public String calculateStatus(InventoryItem item, String today) {
        validateItem(item);
        int daysUntilExpiry = daysBetween(today, item.getExpiryDate());

        if (daysUntilExpiry < 0) {
            return InventoryItem.STATUS_EXPIRED;
        }
        if (daysUntilExpiry <= NEAR_EXPIRY_THRESHOLD_DAYS) {
            return InventoryItem.STATUS_NEAR_EXPIRY;
        }
        return InventoryItem.STATUS_NORMAL;
    }

    /**
     * Recalculates an item's status using today's real date and writes it back to the item.
     */
    public void updateItemStatus(InventoryItem item) {
        item.setStatus(calculateStatus(item));
    }

    /**
     * Recalculates an item's status using a custom today date and writes it back to the item.
     * This version is mainly useful for tests or fixed demo dates.
     */
    public void updateItemStatus(InventoryItem item, String today) {
        item.setStatus(calculateStatus(item, today));
    }

    /**
     * Recalculates an item's expiry date from its ingredient name, category, and purchase date.
     */
    public void updateExpiryDate(InventoryItem item) {
        validateItem(item);
        item.setExpiryDate(estimateExpiryDate(
                item.getIngredientName(),
                item.getCategory(),
                item.getPurchaseDate()
        ));
    }

    /**
     * Recalculates both expiry date and status for one inventory item.
     */
    public void refreshItem(InventoryItem item) {
        updateExpiryDate(item);
        updateItemStatus(item);
    }

    /**
     * Recalculates expiry date and status for a list of inventory items.
     */
    public void refreshItems(List<InventoryItem> items) {
        if (items == null) {
            return;
        }
        for (InventoryItem item : items) {
            refreshItem(item);
        }
    }

    /**
     * Returns how many days remain before the item expires, based on today's real date.
     */
    public int getDaysUntilExpiry(InventoryItem item) {
        return getDaysUntilExpiry(item, getTodayString());
    }

    /**
     * Returns how many days remain before the item expires, based on a custom today date.
     * A negative number means the item is already expired.
     */
    public int getDaysUntilExpiry(InventoryItem item, String today) {
        validateItem(item);
        return daysBetween(today, item.getExpiryDate());
    }

    /**
     * Returns all expired items using today's real date.
     */
    public List<InventoryItem> getExpiredItems(List<InventoryItem> items) {
        return getExpiredItems(items, getTodayString());
    }

    /**
     * Returns all expired items using a custom today date.
     */
    public List<InventoryItem> getExpiredItems(List<InventoryItem> items, String today) {
        List<InventoryItem> expiredItems = new ArrayList<>();
        if (items == null) {
            return expiredItems;
        }

        for (InventoryItem item : items) {
            if (InventoryItem.STATUS_EXPIRED.equals(calculateStatus(item, today))) {
                expiredItems.add(item);
            }
        }

        return expiredItems;
    }

    /**
     * Returns all near-expiry items using today's real date.
     */
    public List<InventoryItem> getNearExpiryItems(List<InventoryItem> items) {
        return getNearExpiryItems(items, getTodayString());
    }

    /**
     * Returns all near-expiry items using a custom today date.
     */
    public List<InventoryItem> getNearExpiryItems(List<InventoryItem> items, String today) {
        List<InventoryItem> nearExpiryItems = new ArrayList<>();
        if (items == null) {
            return nearExpiryItems;
        }

        for (InventoryItem item : items) {
            if (InventoryItem.STATUS_NEAR_EXPIRY.equals(calculateStatus(item, today))) {
                nearExpiryItems.add(item);
            }
        }

        return nearExpiryItems;
    }

    /**
     * Returns all items that should be shown in reminders.
     * This includes both expired and near-expiry items.
     */
    public List<InventoryItem> getReminderItems(List<InventoryItem> items) {
        return getReminderItems(items, getTodayString());
    }

    /**
     * Returns reminder items using a custom today date.
     * This version is useful for tests or controlled demo examples.
     */
    public List<InventoryItem> getReminderItems(List<InventoryItem> items, String today) {
        List<InventoryItem> reminderItems = new ArrayList<>();
        if (items == null) {
            return reminderItems;
        }

        for (InventoryItem item : items) {
            String status = calculateStatus(item, today);
            if (InventoryItem.STATUS_EXPIRED.equals(status)
                    || InventoryItem.STATUS_NEAR_EXPIRY.equals(status)) {
                reminderItems.add(item);
            }
        }

        return reminderItems;
    }

    /**
     * Looks up an ingredient by name from the static InventoryDatabase.
     */
    private Ingredient findIngredientByName(String ingredientName) {
        String targetName = normalize(ingredientName);
        for (Ingredient ingredient : InventoryDatabase.getIngredients()) {
            if (normalize(ingredient.getIngredientName()).equals(targetName)) {
                return ingredient;
            }
        }
        return null;
    }

    /**
     * Calculates the average shelf life for a category in InventoryDatabase.
     * This is a fallback when the exact ingredient name is not found.
     */
    private Integer getAverageShelfLifeByCategory(String category) {
        String targetCategory = normalize(normalizeCategory(category));
        int totalShelfLife = 0;
        int matchingCount = 0;

        for (Ingredient ingredient : InventoryDatabase.getIngredients()) {
            if (normalize(ingredient.getCategory()).equals(targetCategory)) {
                totalShelfLife += ingredient.getDefaultShelfLifeDays();
                matchingCount++;
            }
        }

        if (matchingCount == 0) {
            return null;
        }
        return Math.round((float) totalShelfLife / matchingCount);
    }

    private String normalizeCategory(String category) {
        if (LEGACY_CATEGORY_PROTEIN.equals(category)) {
            return CATEGORY_MEAT;
        }
        return category;
    }

    /**
     * Adds a number of days to a date string and returns the new date string.
     */
    private String addDays(String date, int days) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(parseDate(date));
        calendar.add(Calendar.DAY_OF_MONTH, days);
        return formatDate(calendar.getTime());
    }

    /**
     * Calculates the day difference from startDate to endDate.
     */
    private int daysBetween(String startDate, String endDate) {
        Date start = parseDate(startDate);
        Date end = parseDate(endDate);
        long differenceMillis = end.getTime() - start.getTime();
        return (int) TimeUnit.MILLISECONDS.toDays(differenceMillis);
    }

    /**
     * Gets today's date in yyyy-MM-dd format.
     */
    private String getTodayString() {
        return formatDate(new Date());
    }

    /**
     * Converts a date string into a Date object.
     * Invalid formats will throw an IllegalArgumentException.
     */
    private Date parseDate(String date) {
        try {
            return createDateFormat().parse(date);
        } catch (ParseException e) {
            throw new IllegalArgumentException("Date must use yyyy-MM-dd format: " + date, e);
        }
    }

    /**
     * Converts a Date object back into a yyyy-MM-dd string.
     */
    private String formatDate(Date date) {
        return createDateFormat().format(date);
    }

    /**
     * Creates a strict date formatter for yyyy-MM-dd dates.
     */
    private SimpleDateFormat createDateFormat() {
        SimpleDateFormat format = new SimpleDateFormat(DATE_PATTERN, Locale.US);
        format.setLenient(false);
        return format;
    }

    /**
     * Checks that the caller did not pass a null InventoryItem.
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
