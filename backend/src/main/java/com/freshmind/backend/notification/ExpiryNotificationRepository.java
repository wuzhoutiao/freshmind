package com.freshmind.backend.notification;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExpiryNotificationRepository extends JpaRepository<ExpiryNotification, UUID> {

    boolean existsByOwnerIdAndInventoryItemId(UUID ownerId, UUID inventoryItemId);

    List<ExpiryNotification> findAllByOwnerIdOrderByReadAscCreatedAtDesc(UUID ownerId);

    Optional<ExpiryNotification> findByIdAndOwnerId(UUID id, UUID ownerId);
}
