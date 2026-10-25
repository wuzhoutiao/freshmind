package com.freshmind.backend.notification;

import com.freshmind.backend.session.SessionAuthenticationInterceptor;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications")
public class ExpiryNotificationController {

    private final ExpiryNotificationService service;

    public ExpiryNotificationController(ExpiryNotificationService service) {
        this.service = service;
    }

    @GetMapping
    public List<ExpiryNotificationResponse> list(
            @RequestAttribute(SessionAuthenticationInterceptor.USER_ID_ATTRIBUTE) UUID userId) {
        return service.list(userId);
    }

    @PatchMapping("/{notificationId}/read")
    public ExpiryNotificationResponse markRead(
            @RequestAttribute(SessionAuthenticationInterceptor.USER_ID_ATTRIBUTE) UUID userId,
            @PathVariable UUID notificationId) {
        return service.markRead(userId, notificationId);
    }
}
