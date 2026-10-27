package com.freshmind.backend.inventory;

import static org.assertj.core.api.Assertions.assertThat;

import com.freshmind.backend.common.config.FreshMindProperties;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ExpiryStatusCalculatorTest {

    private ExpiryStatusCalculator calculator;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-01-15T08:00:00Z"), ZoneOffset.UTC);
        FreshMindProperties properties = new FreshMindProperties(
                "UTC",
                2,
                new FreshMindProperties.Recommendation(0.60, 0.20),
                Duration.ofHours(1));
        calculator = new ExpiryStatusCalculator(clock, properties);
    }

    @Test
    void expiryBeforeTodayIsExpired() {
        assertThat(calculator.status(LocalDate.of(2026, 1, 14))).isEqualTo(InventoryStatus.EXPIRED);
    }

    @Test
    void expiryTodayIsNearExpiry() {
        assertThat(calculator.status(LocalDate.of(2026, 1, 15))).isEqualTo(InventoryStatus.NEAR_EXPIRY);
    }

    @Test
    void twoDaysAwayIsStillNearExpiry() {
        assertThat(calculator.status(LocalDate.of(2026, 1, 17))).isEqualTo(InventoryStatus.NEAR_EXPIRY);
    }

    @Test
    void threeDaysAwayIsFresh() {
        assertThat(calculator.status(LocalDate.of(2026, 1, 18))).isEqualTo(InventoryStatus.FRESH);
    }
}

