package com.freshmind.backend.session;

import com.freshmind.backend.common.config.FreshMindProperties;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@Profile("!test & !local")
public class RedisSessionStore implements SessionStore {

    private static final String KEY_PREFIX = "session:";

    private final StringRedisTemplate redisTemplate;
    private final FreshMindProperties properties;

    public RedisSessionStore(StringRedisTemplate redisTemplate, FreshMindProperties properties) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
    }

    @Override
    public String create(UUID userId) {
        String token = UUID.randomUUID().toString().replace("-", "");
        redisTemplate.opsForValue().set(key(token), userId.toString(), properties.sessionTtl());
        return token;
    }

    @Override
    public Optional<UUID> findUserId(String token) {
        String value = redisTemplate.opsForValue().get(key(token));
        if (value == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(UUID.fromString(value));
        } catch (IllegalArgumentException exception) {
            redisTemplate.delete(key(token));
            return Optional.empty();
        }
    }

    @Override
    public void delete(String token) {
        redisTemplate.delete(key(token));
    }

    private String key(String token) {
        return KEY_PREFIX + token;
    }
}
