package com.freshmind.backend.dashboard;

import com.freshmind.backend.inventory.InventoryItemResponse;
import java.time.Instant;
import java.util.List;

public record DashboardResponse(
        long totalItems,
        long freshItems,
        long nearExpiryItems,
        long expiredItems,
        List<InventoryItemResponse> urgentItems,
        Instant generatedAt) {
}

