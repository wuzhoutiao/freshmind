package com.freshmind.backend.session;

import java.util.Optional;
import java.util.UUID;

public interface SessionStore {

    String create(UUID userId);

    Optional<UUID> findUserId(String token);

    void delete(String token);
}
