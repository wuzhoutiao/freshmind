package com.freshmind.backend.catalog;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IngredientCatalogRepository extends JpaRepository<IngredientCatalogItem, Long> {
    List<IngredientCatalogItem> findAllByOrderByDisplayNameAsc();
}

