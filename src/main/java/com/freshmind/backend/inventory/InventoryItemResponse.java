package com.freshmind.backend.inventory;

import com.freshmind.backend.catalog.IngredientCategory;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record InventoryItemResponse(
        UUID id,
        Long catalogIngredientId,
        String ingredientName,
        IngredientCategory category,
        BigDecimal quantity,
        String unit,
        LocalDate purchasedOn,
        LocalDate expiresOn,
        boolean expiryEstimated,
        InventoryStatus status,
        long daysUntilExpiry,
        long version,
        Instant createdAt,
        Instant updatedAt) {
}

