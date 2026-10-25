package com.freshmind.backend.catalog;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ingredients")
public class IngredientCatalogController {

    private final IngredientCatalogService service;

    public IngredientCatalogController(IngredientCatalogService service) {
        this.service = service;
    }

    @GetMapping
    public List<CatalogIngredientResponse> list(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) IngredientCategory category) {
        String normalizedQuery = IngredientNameNormalizer.normalize(query);
        return service.getAll().stream()
                .filter(item -> category == null || item.category() == category)
                .filter(item -> normalizedQuery.isBlank()
                        || IngredientNameNormalizer.normalize(item.name()).contains(normalizedQuery))
                .toList();
    }
}

