package com.freshmind.backend.ai;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class AiRecipePropertiesTest {

    @Test
    void toStringNeverIncludesTheApiKey() {
        AiRecipeProperties properties = new AiRecipeProperties(
                true,
                "https://example.test/v1",
                "secret-value",
                "test-model",
                8,
                Duration.ofSeconds(10));

        assertThat(properties.toString())
                .doesNotContain("secret-value")
                .contains("apiKey=<redacted>");
    }
}
