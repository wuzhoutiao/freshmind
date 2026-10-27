package com.freshmind.backend.session;

import com.freshmind.backend.common.config.FreshMindProperties;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile({"test", "local"})
public class InMemorySessionStore implements SessionStore {

    private final ConcurrentMap<String, SessionEntry> sessions = new ConcurrentHashMap<>();
    private final FreshMindProperties properties;
    private final Clock clock;

    public InMemorySessionStore(FreshMindProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    public String create(UUID userId) {
        SessionEntry entry = new SessionEntry(userId, clock.instant().plus(properties.sessionTtl()));
        String token;
        do {
            token = UUID.randomUUID().toString().replace("-", "");
        } while (sessions.putIfAbsent(token, entry) != null);
        return token;
    }

    @Override
    public Optional<UUID> findUserId(String token) {
        SessionEntry entry = sessions.get(token);
        if (entry == null) {
            return Optional.empty();
        }
        if (!entry.expiresAt().isAfter(clock.instant())) {
            sessions.remove(token, entry);
            return Optional.empty();
        }
        return Optional.of(entry.userId());
    }

    @Override
    public void delete(String token) {
        sessions.remove(token);
    }

    private record SessionEntry(UUID userId, Instant expiresAt) {
    }
}
