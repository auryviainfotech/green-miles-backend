package com.greenmiles.backend.security;

import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Component;

@Component
public class SecurityMetricsService {
    private final AtomicLong authFailures = new AtomicLong(0);
    private final AtomicLong rateLimitBlocks = new AtomicLong(0);
    private final AtomicLong lockoutBlocks = new AtomicLong(0);

    public void incrementAuthFailures() {
        authFailures.incrementAndGet();
    }

    public void incrementRateLimitBlocks() {
        rateLimitBlocks.incrementAndGet();
    }

    public void incrementLockoutBlocks() {
        lockoutBlocks.incrementAndGet();
    }

    public long getAuthFailures() {
        return authFailures.get();
    }

    public long getRateLimitBlocks() {
        return rateLimitBlocks.get();
    }

    public long getLockoutBlocks() {
        return lockoutBlocks.get();
    }
}

