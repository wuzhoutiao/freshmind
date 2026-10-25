package com.freshmind.backend.auth;

import com.freshmind.backend.common.error.ConflictException;
import com.freshmind.backend.common.config.FreshMindProperties;
import com.freshmind.backend.common.error.UnauthorizedException;
import com.freshmind.backend.session.SessionStore;
import com.freshmind.backend.user.UserAccount;
import com.freshmind.backend.user.UserAccountRepository;
import java.time.Clock;
import java.util.Locale;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserAccountRepository users;
    private final PasswordEncoder passwordEncoder;
    private final SessionStore sessions;
    private final FreshMindProperties properties;
    private final Clock clock;

    public AuthService(
            UserAccountRepository users,
            PasswordEncoder passwordEncoder,
            SessionStore sessions,
            FreshMindProperties properties,
            Clock clock) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.sessions = sessions;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (users.existsByEmail(email)) {
            throw new ConflictException("An account with this email already exists.");
        }
        UserAccount user = users.save(new UserAccount(
                email,
                passwordEncoder.encode(request.password()),
                request.displayName().trim(),
                clock.instant()));
        return response(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        UserAccount user = users.findByEmail(normalizeEmail(request.email()))
                .orElseThrow(() -> new UnauthorizedException("Email or password is incorrect."));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new UnauthorizedException("Email or password is incorrect.");
        }
        return response(user);
    }

    public void logout(String token) {
        sessions.delete(token);
    }

    private AuthResponse response(UserAccount user) {
        String token = sessions.create(user.getId());
        return new AuthResponse(
                token,
                properties.sessionTtl().toSeconds(),
                new AuthResponse.UserView(user.getId(), user.getEmail(), user.getDisplayName()));
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}

