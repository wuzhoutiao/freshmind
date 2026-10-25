package com.example.freshmind;

import com.example.freshmind.model.InventoryItem;
import com.example.freshmind.service.ExpiryReminderService;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class ExpiryReminderServiceTest {

    @Test
    public void estimateExpiryDate_shouldUseIngredientShelfLifeFromDatabase() {
        ExpiryReminderService service = new ExpiryReminderService();

        String expiryDate = service.estimateExpiryDate("Egg", "Others", "2026-05-01");

        assertEquals("2026-05-22", expiryDate);
    }

    @Test
    public void calculateStatus_shouldReturnExpiredWhenExpiryDateIsBeforeToday() {
        ExpiryReminderService service = new ExpiryReminderService();
        InventoryItem item = createItem("Milk", "2026-05-01");

        String status = service.calculateStatus(item, "2026-05-02");

        assertEquals(InventoryItem.STATUS_EXPIRED, status);
    }

    @Test
    public void calculateStatus_shouldReturnNearExpiryWithinThreshold() {
        ExpiryReminderService service = new ExpiryReminderService();
        InventoryItem item = createItem("Milk", "2026-05-12");

        String status = service.calculateStatus(item, "2026-05-10");

        assertEquals(InventoryItem.STATUS_NEAR_EXPIRY, status);
    }

    @Test
    public void calculateStatus_shouldReturnNormalWhenNotNearExpiry() {
        ExpiryReminderService service = new ExpiryReminderService();
        InventoryItem item = createItem("Milk", "2026-05-20");

        String status = service.calculateStatus(item, "2026-05-10");

        assertEquals(InventoryItem.STATUS_NORMAL, status);
    }

    @Test
    public void getReminderItems_shouldReturnExpiredAndNearExpiryItems() {
        ExpiryReminderService service = new ExpiryReminderService();
        List<InventoryItem> items = new ArrayList<>();
        items.add(createItem("Expired Milk", "2026-05-01"));
        items.add(createItem("Near Egg", "2026-05-12"));
        items.add(createItem("Fresh Rice", "2026-06-01"));

        List<InventoryItem> reminderItems = service.getReminderItems(items, "2026-05-10");

        assertEquals(2, reminderItems.size());
    }

    @Test
    public void createItemWithEstimatedExpiry_shouldSetExpiryDateAndStatus() {
        ExpiryReminderService service = new ExpiryReminderService();

        InventoryItem item = service.createItemWithEstimatedExpiry(
                1,
                "Milk",
                "Others",
                "1 bottle",
                "2026-05-01"
        );

        assertEquals("2026-05-08", item.getExpiryDate());
    }

    private InventoryItem createItem(String name, String expiryDate) {
        return new InventoryItem(1, name, "Others", "1 bottle",
                "2026-05-01", expiryDate);
    }
}
