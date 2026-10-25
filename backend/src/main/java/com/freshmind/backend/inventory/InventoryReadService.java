package com.freshmind.backend.inventory;

import com.freshmind.backend.catalog.IngredientCategory;
import com.freshmind.backend.catalog.IngredientNameNormalizer;
import com.freshmind.backend.common.config.FreshMindProperties;
import com.freshmind.backend.common.error.InvalidRequestException;
import com.freshmind.backend.common.error.ResourceNotFoundException;
import com.freshmind.backend.common.web.PageResponse;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryReadService {

    private static final Map<String, String> SORT_FIELDS = Map.of(
            "expiry", "expiresOn",
            "name", "ingredientName",
            "created", "createdAt",
            "updated", "updatedAt");

    private final InventoryItemRepository items;
    private final InventoryMapper mapper;
    private final Clock clock;
    private final FreshMindProperties properties;

    public InventoryReadService(
            InventoryItemRepository items,
            InventoryMapper mapper,
            Clock clock,
            FreshMindProperties properties) {
        this.items = items;
        this.mapper = mapper;
        this.clock = clock;
        this.properties = properties;
    }

    @Transactional(readOnly = true)
    public InventoryItemResponse get(UUID ownerId, UUID itemId) {
        return mapper.toResponse(findOwned(ownerId, itemId));
    }

    @Transactional(readOnly = true)
    public PageResponse<InventoryItemResponse> list(
            UUID ownerId,
            String query,
            IngredientCategory category,
            InventoryStatus status,
            int page,
            int size,
            String sortBy,
            Sort.Direction direction) {
        String property = SORT_FIELDS.get(sortBy);
        if (property == null) {
            throw new InvalidRequestException("sortBy must be one of: " + String.join(", ", SORT_FIELDS.keySet()));
        }
        Specification<InventoryItem> specification = ownerIs(ownerId);
        if (query != null && !query.isBlank()) {
            specification = specification.and(nameContains(IngredientNameNormalizer.normalize(query)));
        }
        if (category != null) {
            specification = specification.and(categoryIs(category));
        }
        if (status != null) {
            specification = specification.and(statusIs(status, LocalDate.now(clock)));
        }
        PageRequest pageable = PageRequest.of(page, size, Sort.by(direction, property));
        Page<InventoryItem> result = items.findAll(specification, pageable);
        return PageResponse.from(result, mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public List<InventoryItem> findAllForRecommendation(UUID ownerId) {
        return items.findAllByOwner_Id(ownerId);
    }

    private InventoryItem findOwned(UUID ownerId, UUID itemId) {
        return items.findByIdAndOwner_Id(itemId, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory item was not found."));
    }

    private Specification<InventoryItem> ownerIs(UUID ownerId) {
        return (root, query, builder) -> builder.equal(root.get("owner").get("id"), ownerId);
    }

    private Specification<InventoryItem> nameContains(String queryText) {
        return (root, query, builder) -> builder.like(root.get("normalizedName"), "%" + queryText + "%");
    }

    private Specification<InventoryItem> categoryIs(IngredientCategory category) {
        return (root, query, builder) -> builder.equal(root.get("category"), category);
    }

    private Specification<InventoryItem> statusIs(InventoryStatus status, LocalDate today) {
        return switch (status) {
            case EXPIRED -> (root, query, builder) -> builder.lessThan(root.get("expiresOn"), today);
            case NEAR_EXPIRY -> (root, query, builder) -> builder.between(
                    root.get("expiresOn"), today, today.plusDays(properties.nearExpiryDays()));
            case FRESH -> (root, query, builder) -> builder.greaterThan(
                    root.get("expiresOn"), today.plusDays(properties.nearExpiryDays()));
        };
    }
}

