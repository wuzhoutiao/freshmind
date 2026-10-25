package com.freshmind.backend.notification;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(
        name = "expiry_notifications",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_notification_owner_inventory",
                columnNames = {"owner_id", "inventory_item_id"}))
public class ExpiryNotification {

    @Id
    private UUID id;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(name = "inventory_item_id", nullable = false)
    private UUID inventoryItemId;

    @Column(name = "ingredient_name", nullable = false, length = 100)
    private String ingredientName;

    @Column(name = "expires_on", nullable = false)
    private LocalDate expiresOn;

    @Column(nullable = false, length = 300)
    private String message;

    @Column(name = "is_read", nullable = false)
    private boolean read;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "read_at")
    private Instant readAt;

    protected ExpiryNotification() {
    }

    public ExpiryNotification(
            UUID id,
            UUID ownerId,
            UUID inventoryItemId,
            String ingredientName,
            LocalDate expiresOn,
            String message,
            Instant createdAt) {
        this.id = id;
        this.ownerId = ownerId;
        this.inventoryItemId = inventoryItemId;
        this.ingredientName = ingredientName;
        this.expiresOn = expiresOn;
        this.message = message;
        this.createdAt = createdAt;
    }

    public void markRead(Instant now) {
        if (!read) {
            read = true;
            readAt = now;
        }
    }

    public UUID getId() {
        return id;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public UUID getInventoryItemId() {
        return inventoryItemId;
    }

    public String getIngredientName() {
        return ingredientName;
    }

    public LocalDate getExpiresOn() {
        return expiresOn;
    }

    public String getMessage() {
        return message;
    }

    public boolean isRead() {
        return read;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getReadAt() {
        return readAt;
    }
}
