package com.freshmind.backend.inventory;

import java.math.BigDecimal;
import java.util.UUID;

public record ConsumptionResponse(
        UUID itemId,
        BigDecimal remainingQuantity,
        boolean depleted) {
}

