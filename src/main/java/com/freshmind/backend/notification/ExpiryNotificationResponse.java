package com.freshmind.backend.notification;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ExpiryNotificationResponse(
        UUID id,
        UUID inventoryItemId,
        String ingredientName,
        LocalDate expiresOn,
        String message,
        boolean read,
        Instant createdAt,
        Instant readAt) {
}
