package com.freshmind.backend.notification;

import com.freshmind.backend.common.error.ResourceNotFoundException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExpiryNotificationService {

    private final ExpiryNotificationRepository repository;
    private final ExpiryNotificationWriter writer;
    private final Clock clock;

    public ExpiryNotificationService(
            ExpiryNotificationRepository repository,
            ExpiryNotificationWriter writer,
            Clock clock) {
        this.repository = repository;
        this.writer = writer;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<ExpiryNotificationResponse> list(UUID ownerId) {
        return repository.findAllByOwnerIdOrderByReadAscCreatedAtDesc(ownerId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public ExpiryNotificationResponse markRead(UUID ownerId, UUID notificationId) {
        ExpiryNotification notification = repository.findByIdAndOwnerId(notificationId, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification was not found."));
        notification.markRead(clock.instant());
        return toResponse(notification);
    }

    /**
     * Returns false when this stock item already has a notification. The unique
     * database constraint also protects against two scheduler instances racing.
     */
    public boolean createIfAbsent(
            UUID ownerId,
            UUID inventoryItemId,
            String ingredientName,
            LocalDate expiresOn) {
        LocalDate today = LocalDate.now(clock);
        String message = messageFor(ingredientName, ChronoUnit.DAYS.between(today, expiresOn));
        try {
            return writer.insertIfMissing(
                    ownerId,
                    inventoryItemId,
                    ingredientName,
                    expiresOn,
                    message,
                    clock.instant());
        } catch (DataIntegrityViolationException duplicateFromConcurrentJob) {
            return false;
        }
    }

    private String messageFor(String ingredientName, long daysUntilExpiry) {
        if (daysUntilExpiry == 0) {
            return ingredientName + " 今天到期，请尽快使用。";
        }
        if (daysUntilExpiry == 1) {
            return ingredientName + " 明天到期，请优先使用。";
        }
        return ingredientName + " 将在 " + daysUntilExpiry + " 天后到期。";
    }

    private ExpiryNotificationResponse toResponse(ExpiryNotification notification) {
        return new ExpiryNotificationResponse(
                notification.getId(),
                notification.getInventoryItemId(),
                notification.getIngredientName(),
                notification.getExpiresOn(),
                notification.getMessage(),
                notification.isRead(),
                notification.getCreatedAt(),
                notification.getReadAt());
    }
}
