package com.freshmind.backend.inventory;

import com.freshmind.backend.common.config.FreshMindProperties;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import org.springframework.stereotype.Component;

@Component
public class ExpiryStatusCalculator {

    private final Clock clock;
    private final FreshMindProperties properties;

    public ExpiryStatusCalculator(Clock clock, FreshMindProperties properties) {
        this.clock = clock;
        this.properties = properties;
    }

    public InventoryStatus status(LocalDate expiresOn) {
        long days = daysUntilExpiry(expiresOn);
        if (days < 0) {
            return InventoryStatus.EXPIRED;
        }
        if (days <= properties.nearExpiryDays()) {
            return InventoryStatus.NEAR_EXPIRY;
        }
        return InventoryStatus.FRESH;
    }

    public long daysUntilExpiry(LocalDate expiresOn) {
        return ChronoUnit.DAYS.between(LocalDate.now(clock), expiresOn);
    }
}

