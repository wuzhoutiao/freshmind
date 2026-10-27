package com.freshmind.backend.common.config;

import com.freshmind.backend.catalog.IngredientCatalogItem;
import com.freshmind.backend.catalog.IngredientCatalogRepository;
import com.freshmind.backend.catalog.IngredientCategory;
import java.util.List;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class SeedDataInitializer implements ApplicationRunner {

    private final IngredientCatalogRepository ingredientCatalog;

    public SeedDataInitializer(IngredientCatalogRepository ingredientCatalog) {
        this.ingredientCatalog = ingredientCatalog;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (ingredientCatalog.count() == 0) {
            ingredientCatalog.saveAll(catalogItems());
        }
    }

    private List<IngredientCatalogItem> catalogItems() {
        return List.of(
                catalog(1, "Rice", IngredientCategory.OTHERS, 365),
                catalog(2, "Noodles", IngredientCategory.OTHERS, 180),
                catalog(3, "Bread", IngredientCategory.OTHERS, 7),
                catalog(4, "Egg", IngredientCategory.OTHERS, 21),
                catalog(5, "Chicken Breast", IngredientCategory.MEAT, 2),
                catalog(6, "Beef", IngredientCategory.MEAT, 3),
                catalog(7, "Salmon", IngredientCategory.MEAT, 2),
                catalog(8, "Tofu", IngredientCategory.OTHERS, 5),
                catalog(9, "Shrimp", IngredientCategory.MEAT, 2),
                catalog(10, "Milk", IngredientCategory.OTHERS, 7),
                catalog(11, "Cheddar Cheese", IngredientCategory.OTHERS, 21),
                catalog(12, "Yogurt", IngredientCategory.OTHERS, 14),
                catalog(13, "Butter", IngredientCategory.OTHERS, 30),
                catalog(14, "Tomato", IngredientCategory.VEGETABLES, 7),
                catalog(15, "Potato", IngredientCategory.VEGETABLES, 30),
                catalog(16, "Onion", IngredientCategory.VEGETABLES, 30),
                catalog(17, "Garlic", IngredientCategory.VEGETABLES, 60),
                catalog(18, "Carrot", IngredientCategory.VEGETABLES, 21),
                catalog(19, "Broccoli", IngredientCategory.VEGETABLES, 7),
                catalog(20, "Spinach", IngredientCategory.VEGETABLES, 5),
                catalog(21, "Mushroom", IngredientCategory.VEGETABLES, 7),
                catalog(22, "Bell Pepper", IngredientCategory.VEGETABLES, 10),
                catalog(23, "Cucumber", IngredientCategory.VEGETABLES, 7),
                catalog(24, "Lettuce", IngredientCategory.VEGETABLES, 5),
                catalog(25, "Apple", IngredientCategory.FRUITS, 30),
                catalog(26, "Banana", IngredientCategory.FRUITS, 7),
                catalog(27, "Lemon", IngredientCategory.FRUITS, 14),
                catalog(28, "Olive Oil", IngredientCategory.OTHERS, 180),
                catalog(29, "Soy Sauce", IngredientCategory.OTHERS, 180),
                catalog(30, "Black Pepper", IngredientCategory.OTHERS, 365));
    }

    private IngredientCatalogItem catalog(
            long id,
            String displayName,
            IngredientCategory category,
            int shelfLifeDays) {
        return new IngredientCatalogItem(
                id,
                displayName,
                displayName.toLowerCase(),
                category,
                shelfLifeDays);
    }
}
