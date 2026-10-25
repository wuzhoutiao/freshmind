package com.example.freshmind;

import com.example.freshmind.model.InventoryItem;
import com.example.freshmind.service.InventoryService;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class InventoryServiceTest {

    @Test
    public void addItem_shouldStoreNewInventoryItem() {
        InventoryService service = new InventoryService();
        InventoryItem item = createItem(1, "Egg", "Others", "6 pcs", "2026-05-01", "2026-05-10");

        boolean added = service.addItem(item);

        assertTrue(added);
        assertEquals(1, service.getItemCount());
        assertEquals("Egg", service.findItemById(1).getIngredientName());
    }

    @Test
    public void addItem_shouldRejectDuplicateId() {
        InventoryService service = new InventoryService();

        assertTrue(service.addItem(createItem(1, "Egg", "Others", "6 pcs", "2026-05-01", "2026-05-10")));
        assertFalse(service.addItem(createItem(1, "Milk", "Others", "1 bottle", "2026-05-02", "2026-05-08")));

        assertEquals(1, service.getItemCount());
        assertEquals("Egg", service.findItemById(1).getIngredientName());
    }

    @Test
    public void updateItem_shouldReplaceExistingItem() {
        InventoryService service = new InventoryService();
        service.addItem(createItem(1, "Egg", "Others", "6 pcs", "2026-05-01", "2026-05-10"));

        boolean updated = service.updateItem(createItem(1, "Egg", "Others", "3 pcs", "2026-05-01", "2026-05-10"));

        assertTrue(updated);
        assertEquals("3 pcs", service.findItemById(1).getQuantity());
    }

    @Test
    public void removeItemById_shouldDeleteExistingItem() {
        InventoryService service = new InventoryService();
        service.addItem(createItem(1, "Egg", "Others", "6 pcs", "2026-05-01", "2026-05-10"));

        boolean removed = service.removeItemById(1);

        assertTrue(removed);
        assertEquals(0, service.getItemCount());
        assertNull(service.findItemById(1));
    }

    @Test
    public void getItemsByCategory_shouldReturnMatchingItemsOnly() {
        InventoryService service = new InventoryService();
        service.addItem(createItem(1, "Chicken Breast", "Meat", "500 g", "2026-05-01", "2026-05-10"));
        service.addItem(createItem(2, "Milk", "Others", "1 bottle", "2026-05-02", "2026-05-08"));

        List<InventoryItem> meatItems = service.getItemsByCategory("meat");

        assertEquals(1, meatItems.size());
        assertEquals("Chicken Breast", meatItems.get(0).getIngredientName());
    }

    @Test
    public void getItemsSortedByExpiryDate_shouldReturnEarliestExpiryFirst() {
        InventoryService service = new InventoryService();
        service.addItem(createItem(1, "Egg", "Others", "6 pcs", "2026-05-01", "2026-05-10"));
        service.addItem(createItem(2, "Milk", "Others", "1 bottle", "2026-05-02", "2026-05-08"));

        List<InventoryItem> sortedItems = service.getItemsSortedByExpiryDate();

        assertEquals("Milk", sortedItems.get(0).getIngredientName());
        assertEquals("Egg", sortedItems.get(1).getIngredientName());
    }

    @Test
    public void getNextAvailableId_shouldReturnMaxIdPlusOne() {
        InventoryService service = new InventoryService();
        service.addItem(createItem(3, "Egg", "Others", "6 pcs", "2026-05-01", "2026-05-10"));
        service.addItem(createItem(7, "Milk", "Others", "1 bottle", "2026-05-02", "2026-05-08"));

        assertEquals(8, service.getNextAvailableId());
    }

    @Test
    public void findItemById_shouldReturnNullWhenMissing() {
        InventoryService service = new InventoryService();

        assertNull(service.findItemById(99));
    }

    @Test
    public void findItemsByName_shouldMatchCaseInsensitiveName() {
        InventoryService service = new InventoryService();
        service.addItem(createItem(1, "Egg", "Others", "6 pcs", "2026-05-01", "2026-05-10"));

        List<InventoryItem> results = service.findItemsByName("egg");

        assertEquals(1, results.size());
        assertEquals("Egg", results.get(0).getIngredientName());
    }

    @Test
    public void getAllItems_shouldReturnCopyOfInternalList() {
        InventoryService service = new InventoryService();
        service.addItem(createItem(1, "Egg", "Others", "6 pcs", "2026-05-01", "2026-05-10"));

        List<InventoryItem> items = service.getAllItems();
        items.clear();

        assertEquals(1, service.getItemCount());
        assertNotNull(service.findItemById(1));
    }

    private InventoryItem createItem(int id, String name, String category, String quantity,
                                     String purchaseDate, String expiryDate) {
        return new InventoryItem(id, name, category, quantity, purchaseDate, expiryDate);
    }
}
