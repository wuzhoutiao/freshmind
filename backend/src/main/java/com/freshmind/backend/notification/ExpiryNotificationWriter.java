package com.freshmind.backend.notification;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Keeps the uniqueness check and insert in one transaction. */
@Service
public class ExpiryNotificationWriter {

    private final ExpiryNotificationRepository repository;

    public ExpiryNotificationWriter(ExpiryNotificationRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public boolean insertIfMissing(
            UUID ownerId,
            UUID inventoryItemId,
            String ingredientName,
            LocalDate expiresOn,
            String message,
            Instant now) {
        if (repository.existsByOwnerIdAndInventoryItemId(ownerId, inventoryItemId)) {
            return false;
        }

        repository.saveAndFlush(new ExpiryNotification(
                UUID.randomUUID(),
                ownerId,
                inventoryItemId,
                ingredientName,
                expiresOn,
                message,
                now));
        return true;
    }
}
