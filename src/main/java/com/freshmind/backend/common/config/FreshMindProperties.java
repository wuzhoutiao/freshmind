package com.freshmind.backend.common.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "freshmind")
public record FreshMindProperties(
        @NotBlank String timeZone,
        @Min(0) int nearExpiryDays,
        @NotNull @Valid Recommendation recommendation,
        @NotNull Duration sessionTtl) {

    public record Recommendation(
            @DecimalMin("0.0") @DecimalMax("1.0") double minimumMatchRate,
            @DecimalMin("0.0") double expiringIngredientBonus) {
    }

}

