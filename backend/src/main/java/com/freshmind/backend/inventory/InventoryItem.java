package com.freshmind.backend.inventory;

import com.freshmind.backend.catalog.IngredientCategory;
import com.freshmind.backend.user.UserAccount;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "inventory_items")
public class InventoryItem {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private UserAccount owner;

    @Column(name = "catalog_ingredient_id")
    private Long catalogIngredientId;

    @Column(name = "ingredient_name", nullable = false, length = 100)
    private String ingredientName;

    @Column(name = "normalized_name", nullable = false, length = 100)
    private String normalizedName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private IngredientCategory category;

    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal quantity;

    @Column(nullable = false, length = 24)
    private String unit;

    @Column(name = "purchased_on", nullable = false)
    private LocalDate purchasedOn;

    @Column(name = "expires_on", nullable = false)
    private LocalDate expiresOn;

    @Column(name = "expiry_estimated", nullable = false)
    private boolean expiryEstimated;

    @Version
    @Column(nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected InventoryItem() {
    }

    public InventoryItem(
            UUID id,
            UserAccount owner,
            Long catalogIngredientId,
            String ingredientName,
            String normalizedName,
            IngredientCategory category,
            BigDecimal quantity,
            String unit,
            LocalDate purchasedOn,
            LocalDate expiresOn,
            boolean expiryEstimated,
            Instant now) {
        this.id = id;
        this.owner = owner;
        this.catalogIngredientId = catalogIngredientId;
        this.ingredientName = ingredientName;
        this.normalizedName = normalizedName;
        this.category = category;
        this.quantity = quantity;
        this.unit = unit;
        this.purchasedOn = purchasedOn;
        this.expiresOn = expiresOn;
        this.expiryEstimated = expiryEstimated;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void update(
            Long catalogIngredientId,
            String ingredientName,
            String normalizedName,
            IngredientCategory category,
            BigDecimal quantity,
            String unit,
            LocalDate purchasedOn,
            LocalDate expiresOn,
            Instant now) {
        this.catalogIngredientId = catalogIngredientId;
        this.ingredientName = ingredientName;
        this.normalizedName = normalizedName;
        this.category = category;
        this.quantity = quantity;
        this.unit = unit;
        this.purchasedOn = purchasedOn;
        this.expiresOn = expiresOn;
        this.expiryEstimated = false;
        this.updatedAt = now;
    }

    public void consume(BigDecimal amount, Instant now) {
        this.quantity = this.quantity.subtract(amount);
        this.updatedAt = now;
    }

    public UUID getId() {
        return id;
    }

    public UserAccount getOwner() {
        return owner;
    }

    public Long getCatalogIngredientId() {
        return catalogIngredientId;
    }

    public String getIngredientName() {
        return ingredientName;
    }

    public String getNormalizedName() {
        return normalizedName;
    }

    public IngredientCategory getCategory() {
        return category;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public String getUnit() {
        return unit;
    }

    public LocalDate getPurchasedOn() {
        return purchasedOn;
    }

    public LocalDate getExpiresOn() {
        return expiresOn;
    }

    public boolean isExpiryEstimated() {
        return expiryEstimated;
    }

    public long getVersion() {
        return version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}

