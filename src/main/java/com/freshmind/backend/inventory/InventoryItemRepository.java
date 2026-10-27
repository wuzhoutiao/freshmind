package com.freshmind.backend.inventory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface InventoryItemRepository
        extends JpaRepository<InventoryItem, UUID>, JpaSpecificationExecutor<InventoryItem> {

    Optional<InventoryItem> findByIdAndOwner_Id(UUID id, UUID ownerId);

    List<InventoryItem> findAllByOwner_Id(UUID ownerId);

    @EntityGraph(attributePaths = "owner")
    List<InventoryItem> findAllByExpiresOnBetweenAndQuantityGreaterThan(
            LocalDate startDate,
            LocalDate endDate,
            BigDecimal minimumQuantity);

    long countByOwner_Id(UUID ownerId);
}

