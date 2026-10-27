package com.freshmind.backend.common.config;

import java.time.Duration;
import java.util.Map;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;

/**
 * Uses Redis for shared application caches outside the local and test profiles.
 * Local development and tests intentionally fall back to Boot's simple cache.
 */
@Configuration
@Profile("!test & !local")
public class RedisCacheConfig {

    private static final Duration CACHE_TTL = Duration.ofMinutes(30);
    private static final Duration AI_RECIPE_CACHE_TTL = Duration.ofMinutes(15);

    @Bean
    CacheManager redisCacheManager(RedisConnectionFactory connectionFactory) {
        var typeValidator = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType("com.freshmind.backend.")
                .allowIfSubType("java.util.")
                .allowIfSubType("java.math.")
                .allowIfSubTypeIsArray()
                .build();
        var valueSerializer = GenericJacksonJsonRedisSerializer.builder()
                .enableDefaultTyping(typeValidator)
                .build();
        RedisCacheConfiguration defaults = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(CACHE_TTL)
                .disableCachingNullValues()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(
                        new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(
                        valueSerializer));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaults)
                .withInitialCacheConfigurations(Map.of(
                        "aiRecipeCandidates", defaults.entryTtl(AI_RECIPE_CACHE_TTL)))
                .build();
    }
}
