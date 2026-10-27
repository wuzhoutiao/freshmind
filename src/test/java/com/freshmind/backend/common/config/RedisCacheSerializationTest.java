package com.freshmind.backend.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.freshmind.backend.ai.GeneratedIngredient;
import com.freshmind.backend.ai.GeneratedRecipe;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;

class RedisCacheSerializationTest {

    @Test
    void aiCandidatesCanRoundTripThroughTheRedisSerializer() {
        var typeValidator = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType("com.freshmind.backend.")
                .allowIfSubType("java.util.")
                .allowIfSubType("java.math.")
                .allowIfSubTypeIsArray()
                .build();
        var serializer = GenericJacksonJsonRedisSerializer.builder()
                .enableDefaultTyping(typeValidator)
                .build();
        List<GeneratedRecipe> candidates = new ArrayList<>(List.of(new GeneratedRecipe(
                "Tomato Egg Rice",
                "A quick meal.",
                15,
                new ArrayList<>(List.of(
                        new GeneratedIngredient("Rice", BigDecimal.ONE, "bowl"),
                        new GeneratedIngredient("Egg", new BigDecimal("2"), "pcs"))),
                new ArrayList<>(List.of("Cook the rice.", "Add the egg.")))));

        byte[] bytes = serializer.serialize(candidates);
        Object restored = serializer.deserialize(bytes);

        assertThat(restored).isInstanceOf(List.class);
        assertThat((List<?>) restored).hasSize(1);
        assertThat(((List<?>) restored).get(0)).isEqualTo(candidates.get(0));
    }
}
