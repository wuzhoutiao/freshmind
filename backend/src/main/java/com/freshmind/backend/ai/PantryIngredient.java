package com.freshmind.backend.ai;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PantryIngredient(
        String name,
        BigDecimal quantity,
        String unit,
        LocalDate expiresOn,
        boolean nearExpiry) {
}
