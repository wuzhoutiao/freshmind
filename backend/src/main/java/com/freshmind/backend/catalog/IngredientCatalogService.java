package com.freshmind.backend.catalog;

import java.util.List;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IngredientCatalogService {

    private final IngredientCatalogRepository repository;

    public IngredientCatalogService(IngredientCatalogRepository repository) {
        this.repository = repository;
    }

    @Cacheable(cacheNames = "ingredientCatalog", key = "'all'")
    @Transactional(readOnly = true)
    public List<CatalogIngredientResponse> getAll() {
        return repository.findAllByOrderByDisplayNameAsc().stream()
                .map(item -> new CatalogIngredientResponse(
                        item.getId(),
                        item.getDisplayName(),
                        item.getCategory(),
                        item.getDefaultShelfLifeDays()))
                .toList();
    }
}

