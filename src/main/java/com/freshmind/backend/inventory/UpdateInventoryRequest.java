package com.freshmind.backend.inventory;

import com.freshmind.backend.catalog.IngredientCategory;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record UpdateInventoryRequest(
        @NotBlank @Size(max = 100) String ingredientName,
        @NotNull IngredientCategory category,
        @NotNull @DecimalMin(value = "0.001") @Digits(integer = 9, fraction = 3) BigDecimal quantity,
        @NotBlank @Size(max = 24) String unit,
        @NotNull LocalDate purchasedOn,
        @NotNull LocalDate expiresOn) {
}

