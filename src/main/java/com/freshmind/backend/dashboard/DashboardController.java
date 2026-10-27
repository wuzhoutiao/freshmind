package com.freshmind.backend.dashboard;

import com.freshmind.backend.session.SessionAuthenticationInterceptor;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

    private final DashboardService service;

    public DashboardController(DashboardService service) {
        this.service = service;
    }

    @GetMapping
    public DashboardResponse get(
            @RequestAttribute(SessionAuthenticationInterceptor.USER_ID_ATTRIBUTE) UUID userId) {
        return service.get(userId);
    }
}

