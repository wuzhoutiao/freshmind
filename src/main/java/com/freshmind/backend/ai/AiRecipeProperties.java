package com.freshmind.backend.ai;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "freshmind.ai")
public record AiRecipeProperties(
        boolean enabled,
        @NotBlank String baseUrl,
        String apiKey,
        @NotBlank String model,
        @Min(1) @Max(12) int candidateCount,
        @NotNull Duration timeout) {

    public boolean configured() {
        return enabled && apiKey != null && !apiKey.isBlank();
    }

    @Override
    public String toString() {
        return "AiRecipeProperties[enabled=" + enabled
                + ", baseUrl=" + baseUrl
                + ", apiKey=<redacted>"
                + ", model=" + model
                + ", candidateCount=" + candidateCount
                + ", timeout=" + timeout + "]";
    }
}
