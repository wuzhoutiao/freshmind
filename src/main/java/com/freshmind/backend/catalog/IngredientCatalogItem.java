package com.freshmind.backend.catalog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "ingredient_catalog")
public class IngredientCatalogItem {

    @Id
    private Long id;

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    @Column(name = "normalized_name", nullable = false, unique = true, length = 100)
    private String normalizedName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private IngredientCategory category;

    @Column(name = "default_shelf_life_days", nullable = false)
    private int defaultShelfLifeDays;

    protected IngredientCatalogItem() {
    }

    public IngredientCatalogItem(
            Long id,
            String displayName,
            String normalizedName,
            IngredientCategory category,
            int defaultShelfLifeDays) {
        this.id = id;
        this.displayName = displayName;
        this.normalizedName = normalizedName;
        this.category = category;
        this.defaultShelfLifeDays = defaultShelfLifeDays;
    }

    public Long getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getNormalizedName() {
        return normalizedName;
    }

    public IngredientCategory getCategory() {
        return category;
    }

    public int getDefaultShelfLifeDays() {
        return defaultShelfLifeDays;
    }
}

