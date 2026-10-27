package com.freshmind.backend.notification;

import com.freshmind.backend.common.config.FreshMindProperties;
import com.freshmind.backend.inventory.InventoryItem;
import com.freshmind.backend.inventory.InventoryItemRepository;
import jakarta.annotation.PreDestroy;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ExpiryNotificationJob {

    private static final Logger log = LoggerFactory.getLogger(ExpiryNotificationJob.class);

    private final InventoryItemRepository inventoryItems;
    private final ExpiryNotificationService notifications;
    private final FreshMindProperties properties;
    private final Clock clock;
    private final ThreadPoolExecutor executor;

    public ExpiryNotificationJob(
            InventoryItemRepository inventoryItems,
            ExpiryNotificationService notifications,
            FreshMindProperties properties,
            Clock clock) {
        this.inventoryItems = inventoryItems;
        this.notifications = notifications;
        this.properties = properties;
        this.clock = clock;
        this.executor = new ThreadPoolExecutor(
                2,
                4,
                60,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(100),
                notificationThreadFactory(),
                new ThreadPoolExecutor.CallerRunsPolicy());
    }

    @Scheduled(
            cron = "${freshmind.notification-cron:0 0 8 * * *}",
            zone = "${freshmind.time-zone:Asia/Shanghai}")
    public void generateDailyNotifications() {
        LocalDate today = LocalDate.now(clock);
        LocalDate lastNearExpiryDate = today.plusDays(properties.nearExpiryDays());
        List<NotificationCandidate> candidates = inventoryItems
                .findAllByExpiresOnBetweenAndQuantityGreaterThan(
                        today, lastNearExpiryDate, BigDecimal.ZERO)
                .stream()
                .map(item -> new NotificationCandidate(
                        item.getId(),
                        item.getOwner().getId(),
                        item.getIngredientName(),
                        item.getExpiresOn()))
                .toList();

        AtomicInteger created = new AtomicInteger();
        List<CompletableFuture<Void>> tasks = candidates.stream()
                .map(candidate -> CompletableFuture.runAsync(() -> {
                    if (notifications.createIfAbsent(
                            candidate.ownerId(),
                            candidate.inventoryItemId(),
                            candidate.ingredientName(),
                            candidate.expiresOn())) {
                        created.incrementAndGet();
                    }
                }, executor).exceptionally(error -> {
                    log.warn("Failed to create expiry notification for inventory item {}",
                            candidate.inventoryItemId(), error);
                    return null;
                }))
                .toList();

        CompletableFuture.allOf(tasks.toArray(CompletableFuture[]::new)).join();
        log.info("Expiry notification scan finished: candidates={}, created={}",
                candidates.size(), created.get());
    }

    @PreDestroy
    void shutdownExecutor() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                executor.shutdownNow();
                if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                    log.warn("Expiry notification executor did not terminate cleanly");
                }
            }
        } catch (InterruptedException interrupted) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private ThreadFactory notificationThreadFactory() {
        AtomicInteger sequence = new AtomicInteger();
        return task -> {
            Thread thread = new Thread(task, "expiry-notification-" + sequence.incrementAndGet());
            thread.setDaemon(false);
            return thread;
        };
    }

    private record NotificationCandidate(
            UUID inventoryItemId,
            UUID ownerId,
            String ingredientName,
            LocalDate expiresOn) {
    }
}
