package com.greenmiles.backend.auth;

import java.time.LocalDateTime;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AuthCleanupScheduler {
    private final SessionRepository sessionRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    public AuthCleanupScheduler(SessionRepository sessionRepository, RefreshTokenRepository refreshTokenRepository) {
        this.sessionRepository = sessionRepository;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Scheduled(fixedDelayString = "${app.security.cleanup.fixed-delay-ms:300000}")
    @Transactional
    public void cleanupExpiredAuthRecords() {
        LocalDateTime now = LocalDateTime.now();
        sessionRepository.deleteByExpiryTimeBefore(now);
        refreshTokenRepository.deleteByExpiryTimeBefore(now);
    }
}

