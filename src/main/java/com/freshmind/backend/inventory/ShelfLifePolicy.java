package com.freshmind.backend.inventory;

import com.freshmind.backend.catalog.CatalogIngredientResponse;
import com.freshmind.backend.catalog.IngredientCatalogService;
import com.freshmind.backend.catalog.IngredientCategory;
import com.freshmind.backend.catalog.IngredientNameNormalizer;
import com.freshmind.backend.common.error.InvalidRequestException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class ShelfLifePolicy {

    private static final int GLOBAL_DEFAULT_DAYS = 7;
    private static final Map<IngredientCategory, Integer> CATEGORY_DEFAULTS = categoryDefaults();

    private final IngredientCatalogService catalog;
    private final Clock clock;

    public ShelfLifePolicy(IngredientCatalogService catalog, Clock clock) {
        this.catalog = catalog;
        this.clock = clock;
    }

    public PreparedInventory prepare(CreateInventoryRequest request) {
        validatePurchaseDate(request.purchasedOn());
        String name = request.ingredientName().trim().replaceAll("\\s+", " ");
        String normalizedName = IngredientNameNormalizer.normalize(name);
        CatalogIngredientResponse catalogItem = catalog.getAll().stream()
                .filter(item -> IngredientNameNormalizer.normalize(item.name()).equals(normalizedName))
                .findFirst()
                .orElse(null);

        IngredientCategory category = catalogItem != null
                ? catalogItem.category()
                : request.category() == null ? IngredientCategory.OTHERS : request.category();
        int shelfLifeDays = catalogItem != null
                ? catalogItem.defaultShelfLifeDays()
                : CATEGORY_DEFAULTS.getOrDefault(category, GLOBAL_DEFAULT_DAYS);
        boolean estimated = request.expiresOn() == null;
        LocalDate expiresOn = estimated
                ? request.purchasedOn().plusDays(shelfLifeDays)
                : request.expiresOn();
        validateDateRange(request.purchasedOn(), expiresOn);

        return new PreparedInventory(
                catalogItem == null ? null : catalogItem.id(),
                name,
                normalizedName,
                category,
                normalizeQuantity(request.quantity()),
                request.unit().trim(),
                request.purchasedOn(),
                expiresOn,
                estimated);
    }

    public PreparedInventory prepare(UpdateInventoryRequest request) {
        validatePurchaseDate(request.purchasedOn());
        validateDateRange(request.purchasedOn(), request.expiresOn());
        String name = request.ingredientName().trim().replaceAll("\\s+", " ");
        String normalizedName = IngredientNameNormalizer.normalize(name);
        CatalogIngredientResponse catalogItem = catalog.getAll().stream()
                .filter(item -> IngredientNameNormalizer.normalize(item.name()).equals(normalizedName))
                .findFirst()
                .orElse(null);
        return new PreparedInventory(
                catalogItem == null ? null : catalogItem.id(),
                name,
                normalizedName,
                catalogItem == null ? request.category() : catalogItem.category(),
                normalizeQuantity(request.quantity()),
                request.unit().trim(),
                request.purchasedOn(),
                request.expiresOn(),
                false);
    }

    private void validatePurchaseDate(LocalDate purchasedOn) {
        if (purchasedOn.isAfter(LocalDate.now(clock))) {
            throw new InvalidRequestException("Purchase date cannot be in the future.");
        }
    }

    private void validateDateRange(LocalDate purchasedOn, LocalDate expiresOn) {
        if (expiresOn.isBefore(purchasedOn)) {
            throw new InvalidRequestException("Expiry date cannot be before purchase date.");
        }
    }

    private BigDecimal normalizeQuantity(BigDecimal quantity) {
        return quantity.stripTrailingZeros();
    }

    private static Map<IngredientCategory, Integer> categoryDefaults() {
        EnumMap<IngredientCategory, Integer> defaults = new EnumMap<>(IngredientCategory.class);
        defaults.put(IngredientCategory.VEGETABLES, 7);
        defaults.put(IngredientCategory.FRUITS, 7);
        defaults.put(IngredientCategory.MEAT, 3);
        defaults.put(IngredientCategory.OTHERS, GLOBAL_DEFAULT_DAYS);
        return Map.copyOf(defaults);
    }

    public record PreparedInventory(
            Long catalogIngredientId,
            String ingredientName,
            String normalizedName,
            IngredientCategory category,
            BigDecimal quantity,
            String unit,
            LocalDate purchasedOn,
            LocalDate expiresOn,
            boolean expiryEstimated) {
    }
}

