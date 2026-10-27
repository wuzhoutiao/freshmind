package com.freshmind.backend.inventory;

import org.springframework.stereotype.Component;

@Component
public class InventoryMapper {

    private final ExpiryStatusCalculator statusCalculator;

    public InventoryMapper(ExpiryStatusCalculator statusCalculator) {
        this.statusCalculator = statusCalculator;
    }

    public InventoryItemResponse toResponse(InventoryItem item) {
        return new InventoryItemResponse(
                item.getId(),
                item.getCatalogIngredientId(),
                item.getIngredientName(),
                item.getCategory(),
                item.getQuantity(),
                item.getUnit(),
                item.getPurchasedOn(),
                item.getExpiresOn(),
                item.isExpiryEstimated(),
                statusCalculator.status(item.getExpiresOn()),
                statusCalculator.daysUntilExpiry(item.getExpiresOn()),
                item.getVersion(),
                item.getCreatedAt(),
                item.getUpdatedAt());
    }
}

