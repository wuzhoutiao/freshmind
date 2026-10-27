package com.freshmind.backend.session;

import com.freshmind.backend.common.error.UnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class SessionAuthenticationInterceptor implements HandlerInterceptor {

    public static final String TOKEN_HEADER = "X-Token";
    public static final String USER_ID_ATTRIBUTE = "userId";

    private final SessionStore sessions;

    public SessionAuthenticationInterceptor(SessionStore sessions) {
        this.sessions = sessions;
    }

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler) {
        String token = request.getHeader(TOKEN_HEADER);
        if (token == null || token.isBlank()) {
            throw new UnauthorizedException("Please log in first.");
        }
        UUID userId = sessions.findUserId(token.trim())
                .orElseThrow(() -> new UnauthorizedException("The login session is invalid or has expired."));
        request.setAttribute(USER_ID_ATTRIBUTE, userId);
        return true;
    }
}
