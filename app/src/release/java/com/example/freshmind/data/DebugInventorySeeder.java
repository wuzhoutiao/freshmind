package com.example.freshmind.data;

import com.example.freshmind.model.InventoryItem;

import java.util.List;

public final class DebugInventorySeeder {

    private DebugInventorySeeder() {
    }

    public static List<InventoryItem> loadItemsForAppStart(InventoryStorage storage) {
        return storage.loadItems();
    }
}
