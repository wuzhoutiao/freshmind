package com.freshmind.backend.auth;

import java.util.UUID;

public record AuthResponse(
        String token,
        long expiresInSeconds,
        UserView user) {

    public record UserView(UUID id, String email, String displayName) {
    }
}

