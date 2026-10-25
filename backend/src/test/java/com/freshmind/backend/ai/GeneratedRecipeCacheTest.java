package com.freshmind.backend.ai;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

class GeneratedRecipeCacheTest {

    @Test
    void sameInventoryUsesCacheAndChangedFingerprintRegenerates() {
        try (AnnotationConfigApplicationContext context =
                     new AnnotationConfigApplicationContext(CacheTestConfig.class)) {
            GeneratedRecipeCache cache = context.getBean(GeneratedRecipeCache.class);
            CountingClient client = context.getBean(CountingClient.class);
            UUID ownerId = UUID.randomUUID();
            List<PantryIngredient> pantry = List.of(new PantryIngredient(
                    "Tomato", BigDecimal.ONE, "pc", LocalDate.of(2026, 1, 20), false));
            List<String> allowed = List.of("Tomato", "Egg");

            cache.getOrGenerate(ownerId, "fingerprint-1", "model:v1", pantry, allowed);
            cache.getOrGenerate(ownerId, "fingerprint-1", "model:v1", pantry, allowed);
            cache.getOrGenerate(ownerId, "fingerprint-2", "model:v1", pantry, allowed);

            assertThat(client.calls()).isEqualTo(2);
        }
    }

    @Configuration
    @EnableCaching
    static class CacheTestConfig {

        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager("aiRecipeCandidates");
        }

        @Bean
        CountingClient countingClient() {
            return new CountingClient();
        }

        @Bean
        GeneratedRecipeCache generatedRecipeCache(CountingClient client) {
            return new GeneratedRecipeCache(client);
        }
    }

    static class CountingClient implements RecipeGenerationClient {

        private final AtomicInteger calls = new AtomicInteger();

        @Override
        public List<GeneratedRecipe> generate(
                List<PantryIngredient> pantry,
                List<String> allowedIngredients) {
            calls.incrementAndGet();
            return List.of(new GeneratedRecipe(
                    "Tomato Egg",
                    "Test recipe.",
                    10,
                    List.of(
                            new GeneratedIngredient("Tomato", BigDecimal.ONE, "pc"),
                            new GeneratedIngredient("Egg", BigDecimal.ONE, "pc")),
                    List.of("Prepare.", "Cook.")));
        }

        int calls() {
            return calls.get();
        }
    }
}
