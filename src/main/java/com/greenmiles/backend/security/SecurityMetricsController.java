package com.greenmiles.backend.security;

import com.greenmiles.backend.common.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/security")
public class SecurityMetricsController {
    private final SecurityMetricsService securityMetricsService;
    private final LoginAttemptService loginAttemptService;

    public SecurityMetricsController(SecurityMetricsService securityMetricsService, LoginAttemptService loginAttemptService) {
        this.securityMetricsService = securityMetricsService;
        this.loginAttemptService = loginAttemptService;
    }

    @GetMapping("/metrics")
    public ResponseEntity<ApiResponse<SecurityMetricsResponse>> metrics() {
        SecurityMetricsResponse response = new SecurityMetricsResponse(
                securityMetricsService.getAuthFailures(),
                securityMetricsService.getRateLimitBlocks(),
                securityMetricsService.getLockoutBlocks(),
                loginAttemptService.getActiveLockCount());
        return ResponseEntity.ok(ApiResponse.ok("Security metrics fetched", response));
    }

    public record SecurityMetricsResponse(
            long authFailures, long rateLimitBlocks, long lockoutBlocks, long activeLockouts) {}
}

