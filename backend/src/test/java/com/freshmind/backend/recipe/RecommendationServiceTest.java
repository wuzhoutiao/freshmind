package com.freshmind.backend.recipe;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.freshmind.backend.ai.AiRecipeProperties;
import com.freshmind.backend.ai.GeneratedRecipeCache;
import com.freshmind.backend.ai.GeneratedRecipeValidator;
import com.freshmind.backend.ai.RecipeGenerationException;
import com.freshmind.backend.catalog.CatalogIngredientResponse;
import com.freshmind.backend.catalog.IngredientCatalogService;
import com.freshmind.backend.catalog.IngredientCategory;
import com.freshmind.backend.common.config.FreshMindProperties;
import com.freshmind.backend.inventory.ExpiryStatusCalculator;
import com.freshmind.backend.inventory.InventoryItem;
import com.freshmind.backend.inventory.InventoryReadService;
import com.freshmind.backend.inventory.InventoryStatus;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RecommendationServiceTest {

    private IngredientCatalogService catalog;
    private InventoryReadService inventory;
    private ExpiryStatusCalculator statusCalculator;
    private GeneratedRecipeCache cache;
    private RecommendationService service;

    @BeforeEach
    void setUp() {
        catalog = mock(IngredientCatalogService.class);
        inventory = mock(InventoryReadService.class);
        statusCalculator = mock(ExpiryStatusCalculator.class);
        cache = mock(GeneratedRecipeCache.class);
        FreshMindProperties properties = new FreshMindProperties(
                "UTC",
                2,
                new FreshMindProperties.Recommendation(0.60, 0.20),
                Duration.ofHours(1));
        AiRecipeProperties aiProperties = new AiRecipeProperties(
                true,
                "https://example.test/v1",
                "test-key",
                "test-model",
                8,
                Duration.ofSeconds(1));
        service = new RecommendationService(
                catalog,
                inventory,
                statusCalculator,
                properties,
                aiProperties,
                cache,
                new GeneratedRecipeValidator());
    }

    @Test
    void emptyInventoryDoesNotCallTheModelCache() {
        when(catalog.getAll()).thenReturn(catalogItems());
        when(inventory.findAllForRecommendation(any(UUID.class))).thenReturn(List.of());

        assertThat(service.recommend(UUID.randomUUID(), 8)).isEmpty();

        verify(cache, never()).getOrGenerate(any(), any(), any(), any(), any());
    }

    @Test
    void modelFailureReturnsControlledFallback() {
        InventoryItem tomato = inventoryItem("tomato", "pcs", "2", LocalDate.of(2026, 1, 20));
        InventoryItem egg = inventoryItem("egg", "pcs", "6", LocalDate.of(2026, 1, 21));
        when(catalog.getAll()).thenReturn(catalogItems());
        when(inventory.findAllForRecommendation(any(UUID.class))).thenReturn(List.of(tomato, egg));
        when(statusCalculator.status(any(LocalDate.class))).thenReturn(InventoryStatus.FRESH);
        when(cache.getOrGenerate(any(), any(), any(), any(), any()))
                .thenThrow(new RecipeGenerationException("not configured"));

        List<RecommendationResponse> results = service.recommend(UUID.randomUUID(), 8);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).recipe().name()).isEqualTo("Pantry Quick Stir Fry");
        assertThat(results.get(0).matchRate()).isEqualTo(1.0);
    }

    private List<CatalogIngredientResponse> catalogItems() {
        return List.of(
                new CatalogIngredientResponse(1, "Tomato", IngredientCategory.VEGETABLES, 7),
                new CatalogIngredientResponse(2, "Egg", IngredientCategory.OTHERS, 21));
    }

    private InventoryItem inventoryItem(
            String normalizedName,
            String unit,
            String quantity,
            LocalDate expiresOn) {
        InventoryItem item = mock(InventoryItem.class);
        when(item.getNormalizedName()).thenReturn(normalizedName);
        when(item.getUnit()).thenReturn(unit);
        when(item.getQuantity()).thenReturn(new BigDecimal(quantity));
        when(item.getExpiresOn()).thenReturn(expiresOn);
        return item;
    }
}
