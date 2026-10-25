package com.freshmind.backend.inventory;

import com.freshmind.backend.common.error.InvalidRequestException;
import com.freshmind.backend.common.error.ResourceNotFoundException;
import com.freshmind.backend.user.UserAccount;
import com.freshmind.backend.user.UserAccountRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryWriteService {

    private final InventoryItemRepository items;
    private final UserAccountRepository users;
    private final ShelfLifePolicy shelfLifePolicy;
    private final InventoryMapper mapper;
    private final Clock clock;

    public InventoryWriteService(
            InventoryItemRepository items,
            UserAccountRepository users,
            ShelfLifePolicy shelfLifePolicy,
            InventoryMapper mapper,
            Clock clock) {
        this.items = items;
        this.users = users;
        this.shelfLifePolicy = shelfLifePolicy;
        this.mapper = mapper;
        this.clock = clock;
    }

    @Transactional
    public InventoryItemResponse create(UUID ownerId, CreateInventoryRequest request) {
        ShelfLifePolicy.PreparedInventory prepared = shelfLifePolicy.prepare(request);
        UserAccount owner = users.findById(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("User account was not found."));
        InventoryItem item = new InventoryItem(
                UUID.randomUUID(),
                owner,
                prepared.catalogIngredientId(),
                prepared.ingredientName(),
                prepared.normalizedName(),
                prepared.category(),
                prepared.quantity(),
                prepared.unit(),
                prepared.purchasedOn(),
                prepared.expiresOn(),
                prepared.expiryEstimated(),
                clock.instant());
        items.saveAndFlush(item);
        return mapper.toResponse(item);
    }

    @Transactional
    public InventoryItemResponse update(
            UUID ownerId,
            UUID itemId,
            UpdateInventoryRequest request) {
        InventoryItem item = findOwned(ownerId, itemId);
        ShelfLifePolicy.PreparedInventory prepared = shelfLifePolicy.prepare(request);
        item.update(
                prepared.catalogIngredientId(),
                prepared.ingredientName(),
                prepared.normalizedName(),
                prepared.category(),
                prepared.quantity(),
                prepared.unit(),
                prepared.purchasedOn(),
                prepared.expiresOn(),
                clock.instant());
        items.saveAndFlush(item);
        return mapper.toResponse(item);
    }

    @Transactional
    public ConsumptionResponse consume(
            UUID ownerId,
            UUID itemId,
            ConsumeInventoryRequest request) {
        InventoryItem item = findOwned(ownerId, itemId);
        BigDecimal amount = request.amount().stripTrailingZeros();
        int comparison = amount.compareTo(item.getQuantity());
        if (comparison > 0) {
            throw new InvalidRequestException("Consumed quantity cannot exceed the quantity on hand.");
        }

        if (comparison == 0) {
            items.delete(item);
            items.flush();
            return new ConsumptionResponse(itemId, BigDecimal.ZERO, true);
        }

        item.consume(amount, clock.instant());
        items.saveAndFlush(item);
        return new ConsumptionResponse(itemId, item.getQuantity(), false);
    }

    @Transactional
    public void delete(UUID ownerId, UUID itemId) {
        InventoryItem item = findOwned(ownerId, itemId);
        items.delete(item);
        items.flush();
    }

    private InventoryItem findOwned(UUID ownerId, UUID itemId) {
        return items.findByIdAndOwner_Id(itemId, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory item was not found."));
    }

}

