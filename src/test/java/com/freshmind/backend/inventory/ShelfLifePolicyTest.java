package com.freshmind.backend.inventory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.freshmind.backend.catalog.CatalogIngredientResponse;
import com.freshmind.backend.catalog.IngredientCatalogService;
import com.freshmind.backend.catalog.IngredientCategory;
import com.freshmind.backend.common.error.InvalidRequestException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ShelfLifePolicyTest {

    private ShelfLifePolicy policy;

    @BeforeEach
    void setUp() {
        IngredientCatalogService catalog = mock(IngredientCatalogService.class);
        when(catalog.getAll()).thenReturn(List.of(
                new CatalogIngredientResponse(10, "Milk", IngredientCategory.OTHERS, 7)));
        Clock clock = Clock.fixed(Instant.parse("2026-01-15T08:00:00Z"), ZoneOffset.UTC);
        policy = new ShelfLifePolicy(catalog, clock);
    }

    @Test
    void knownIngredientUsesCatalogShelfLife() {
        var prepared = policy.prepare(request(" milk ", null, LocalDate.of(2026, 1, 10), null));

        assertThat(prepared.catalogIngredientId()).isEqualTo(10);
        assertThat(prepared.expiresOn()).isEqualTo(LocalDate.of(2026, 1, 17));
        assertThat(prepared.expiryEstimated()).isTrue();
    }

    @Test
    void unknownOtherIngredientUsesSevenDayFallbackRatherThanCategoryAverage() {
        var prepared = policy.prepare(request("Homemade Sauce", IngredientCategory.OTHERS,
                LocalDate.of(2026, 1, 10), null));

        assertThat(prepared.expiresOn()).isEqualTo(LocalDate.of(2026, 1, 17));
    }

    @Test
    void purchaseDateInFutureIsRejected() {
        assertThatThrownBy(() -> policy.prepare(request(
                "Milk", null, LocalDate.of(2026, 1, 16), null)))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("future");
    }

    @Test
    void expiryBeforePurchaseIsRejected() {
        assertThatThrownBy(() -> policy.prepare(request(
                "Milk", null, LocalDate.of(2026, 1, 10), LocalDate.of(2026, 1, 9))))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("before purchase");
    }

    private CreateInventoryRequest request(
            String name,
            IngredientCategory category,
            LocalDate purchasedOn,
            LocalDate expiresOn) {
        return new CreateInventoryRequest(name, category, new BigDecimal("1"), "pc", purchasedOn, expiresOn);
    }
}

