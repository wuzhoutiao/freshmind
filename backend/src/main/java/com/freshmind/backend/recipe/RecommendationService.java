package com.freshmind.backend.recipe;

import com.freshmind.backend.ai.AiRecipeProperties;
import com.freshmind.backend.ai.GeneratedIngredient;
import com.freshmind.backend.ai.GeneratedRecipe;
import com.freshmind.backend.ai.GeneratedRecipeCache;
import com.freshmind.backend.ai.GeneratedRecipeValidator;
import com.freshmind.backend.ai.PantryIngredient;
import com.freshmind.backend.ai.RecipeGenerationException;
import com.freshmind.backend.catalog.CatalogIngredientResponse;
import com.freshmind.backend.catalog.IngredientCatalogService;
import com.freshmind.backend.catalog.IngredientNameNormalizer;
import com.freshmind.backend.common.config.FreshMindProperties;
import com.freshmind.backend.inventory.ExpiryStatusCalculator;
import com.freshmind.backend.inventory.InventoryItem;
import com.freshmind.backend.inventory.InventoryReadService;
import com.freshmind.backend.inventory.InventoryStatus;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

@Service
public class RecommendationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RecommendationService.class);
    private static final String PROMPT_VERSION = "recipe-v1";

    private final IngredientCatalogService catalog;
    private final InventoryReadService inventory;
    private final ExpiryStatusCalculator statusCalculator;
    private final FreshMindProperties properties;
    private final AiRecipeProperties aiProperties;
    private final GeneratedRecipeCache generatedRecipeCache;
    private final GeneratedRecipeValidator validator;

    public RecommendationService(
            IngredientCatalogService catalog,
            InventoryReadService inventory,
            ExpiryStatusCalculator statusCalculator,
            FreshMindProperties properties,
            AiRecipeProperties aiProperties,
            GeneratedRecipeCache generatedRecipeCache,
            GeneratedRecipeValidator validator) {
        this.catalog = catalog;
        this.inventory = inventory;
        this.statusCalculator = statusCalculator;
        this.properties = properties;
        this.aiProperties = aiProperties;
        this.generatedRecipeCache = generatedRecipeCache;
        this.validator = validator;
    }

    public List<RecommendationResponse> recommend(UUID ownerId, int limit) {
        Map<String, String> canonicalNames = canonicalIngredientNames();
        InventoryContext context = inventoryContext(ownerId, canonicalNames);
        if (context.pantry().isEmpty()) {
            return List.of();
        }

        List<String> allowedIngredients = canonicalNames.values().stream()
                .sorted()
                .toList();
        List<GeneratedRecipe> generated;
        try {
            generated = generatedRecipeCache.getOrGenerate(
                    ownerId,
                    fingerprint(context.pantry()),
                    cacheVersion(allowedIngredients),
                    context.pantry(),
                    allowedIngredients);
        } catch (RecipeGenerationException | DataAccessException | Cache.ValueRetrievalException exception) {
            LOGGER.warn("AI recipe generation is unavailable; using the deterministic fallback.");
            generated = fallbackRecipes(context.pantry());
        }

        List<GeneratedRecipe> validRecipes = validator.validate(generated, canonicalNames);
        if (validRecipes.isEmpty()) {
            LOGGER.warn("AI recipe candidates did not pass validation; using the deterministic fallback.");
            validRecipes = validator.validate(fallbackRecipes(context.pantry()), canonicalNames);
        }

        return validRecipes.stream()
                .map(recipe -> score(recipe, context.available()))
                .filter(result -> result.matchRate() >= properties.recommendation().minimumMatchRate())
                .sorted(Comparator.comparingDouble(RecommendationResponse::score).reversed()
                        .thenComparing(Comparator.comparingDouble(RecommendationResponse::matchRate).reversed())
                        .thenComparing(result -> result.recipe().name()))
                .limit(Math.max(0, limit))
                .toList();
    }

    private Map<String, String> canonicalIngredientNames() {
        Map<String, String> names = new HashMap<>();
        for (CatalogIngredientResponse ingredient : catalog.getAll()) {
            names.put(IngredientNameNormalizer.normalize(ingredient.name()), ingredient.name());
        }
        return Map.copyOf(names);
    }

    private InventoryContext inventoryContext(
            UUID ownerId,
            Map<String, String> canonicalNames) {
        Map<String, InventoryStatus> available = new HashMap<>();
        List<PantryIngredient> pantry = new ArrayList<>();

        for (InventoryItem item : inventory.findAllForRecommendation(ownerId)) {
            InventoryStatus status = statusCalculator.status(item.getExpiresOn());
            if (status == InventoryStatus.EXPIRED || item.getQuantity().signum() <= 0) {
                continue;
            }
            String normalized = IngredientNameNormalizer.normalize(item.getNormalizedName());
            String canonicalName = canonicalNames.get(normalized);
            if (canonicalName == null) {
                continue;
            }
            available.merge(
                    normalized,
                    status,
                    (left, right) -> left == InventoryStatus.NEAR_EXPIRY
                                    || right == InventoryStatus.NEAR_EXPIRY
                            ? InventoryStatus.NEAR_EXPIRY
                            : InventoryStatus.FRESH);
            pantry.add(new PantryIngredient(
                    canonicalName,
                    item.getQuantity(),
                    item.getUnit(),
                    item.getExpiresOn(),
                    status == InventoryStatus.NEAR_EXPIRY));
        }

        pantry.sort(Comparator.comparing(PantryIngredient::name)
                .thenComparing(PantryIngredient::expiresOn)
                .thenComparing(PantryIngredient::unit)
                .thenComparing(PantryIngredient::quantity));
        return new InventoryContext(Map.copyOf(available), List.copyOf(pantry));
    }

    private RecommendationResponse score(
            GeneratedRecipe generated,
            Map<String, InventoryStatus> available) {
        Set<String> required = new LinkedHashSet<>();
        Map<String, String> displayNames = new HashMap<>();
        for (GeneratedIngredient ingredient : generated.ingredients()) {
            String normalized = IngredientNameNormalizer.normalize(ingredient.name());
            required.add(normalized);
            displayNames.put(normalized, ingredient.name());
        }

        List<String> matched = required.stream()
                .filter(available::containsKey)
                .map(displayNames::get)
                .sorted()
                .toList();
        List<String> missing = required.stream()
                .filter(name -> !available.containsKey(name))
                .map(displayNames::get)
                .sorted()
                .toList();
        List<String> nearExpiryUsed = required.stream()
                .filter(name -> available.get(name) == InventoryStatus.NEAR_EXPIRY)
                .map(displayNames::get)
                .sorted()
                .toList();

        double matchRate = (double) matched.size() / required.size();
        double score = matchRate
                + nearExpiryUsed.size() * properties.recommendation().expiringIngredientBonus();
        RecipeResponse recipe = new RecipeResponse(
                generatedId(generated),
                generated.name(),
                generated.description(),
                generated.cookingMinutes(),
                "ai-generated",
                generated.steps(),
                generated.ingredients().stream()
                        .map(ingredient -> new RecipeResponse.IngredientAmount(
                                ingredient.name(), ingredient.amount(), ingredient.unit()))
                        .toList());
        return new RecommendationResponse(
                recipe,
                round(matchRate),
                round(score),
                matched,
                missing,
                nearExpiryUsed,
                explanation(matchRate, nearExpiryUsed.size()));
    }

    private List<GeneratedRecipe> fallbackRecipes(List<PantryIngredient> pantry) {
        Map<String, PantryIngredient> unique = new LinkedHashMap<>();
        pantry.stream()
                .sorted(Comparator.comparing(PantryIngredient::nearExpiry).reversed()
                        .thenComparing(PantryIngredient::expiresOn)
                        .thenComparing(PantryIngredient::name))
                .forEach(item -> unique.putIfAbsent(
                        IngredientNameNormalizer.normalize(item.name()), item));
        List<PantryIngredient> selected = unique.values().stream().limit(5).toList();
        if (selected.size() < 2) {
            return List.of();
        }
        List<GeneratedIngredient> ingredients = selected.stream()
                .map(item -> new GeneratedIngredient(item.name(), BigDecimal.ONE, item.unit()))
                .toList();
        String names = String.join(", ", selected.stream().map(PantryIngredient::name).toList());
        return List.of(new GeneratedRecipe(
                "Pantry Quick Stir Fry",
                "A simple fallback recipe using available ingredients: " + names + ".",
                20,
                ingredients,
                List.of(
                        "Prepare the ingredients and cut them into even pieces.",
                        "Cook everything thoroughly in a pan and season to taste.")));
    }

    private String cacheVersion(List<String> allowedIngredients) {
        return aiProperties.model() + ":" + PROMPT_VERSION + ":" + hashStrings(allowedIngredients);
    }

    private String fingerprint(List<PantryIngredient> pantry) {
        MessageDigest digest = sha256();
        for (PantryIngredient item : pantry) {
            update(digest, item.name());
            update(digest, item.quantity().stripTrailingZeros().toPlainString());
            update(digest, item.unit());
            update(digest, item.expiresOn().toString());
            update(digest, Boolean.toString(item.nearExpiry()));
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    private String hashStrings(List<String> values) {
        MessageDigest digest = sha256();
        values.forEach(value -> update(digest, value));
        return HexFormat.of().formatHex(digest.digest()).substring(0, 16);
    }

    private MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable.", exception);
        }
    }

    private void update(MessageDigest digest, String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        digest.update(ByteBuffer.allocate(Integer.BYTES).putInt(bytes.length).array());
        digest.update(bytes);
    }

    private UUID generatedId(GeneratedRecipe recipe) {
        String identity = recipe.name() + "|" + recipe.ingredients().stream()
                .map(ingredient -> ingredient.name() + ":" + ingredient.amount() + ":" + ingredient.unit())
                .sorted()
                .reduce("", (left, right) -> left + "|" + right);
        return UUID.nameUUIDFromBytes(identity.getBytes(StandardCharsets.UTF_8));
    }

    private String explanation(double matchRate, int nearExpiryCount) {
        int percent = (int) Math.round(matchRate * 100);
        if (nearExpiryCount == 0) {
            return percent + "% of required ingredients are available.";
        }
        return percent + "% match and uses " + nearExpiryCount + " near-expiry ingredient(s).";
    }

    private double round(double value) {
        return Math.round(value * 10_000d) / 10_000d;
    }

    private record InventoryContext(
            Map<String, InventoryStatus> available,
            List<PantryIngredient> pantry) {
    }
}
