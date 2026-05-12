package com.greenmiles.backend.security;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class LoginAttemptService {
    private final ConcurrentHashMap<String, AttemptInfo> attempts = new ConcurrentHashMap<>();
    private final int maxAttempts;
    private final long lockMinutes;

    public LoginAttemptService(
            @Value("${app.security.lockout.max-attempts:5}") int maxAttempts,
            @Value("${app.security.lockout.duration-minutes:15}") long lockMinutes) {
        this.maxAttempts = maxAttempts;
        this.lockMinutes = lockMinutes;
    }

    public boolean isLocked(String identity) {
        AttemptInfo info = attempts.get(identityKey(identity));
        if (info == null) return false;
        if (info.lockedUntil == null) return false;
        if (info.lockedUntil.isAfter(Instant.now())) return true;
        attempts.remove(identityKey(identity));
        return false;
    }

    public void recordFailure(String identity) {
        String key = identityKey(identity);
        attempts.compute(key, (k, existing) -> {
            AttemptInfo info = existing == null ? new AttemptInfo() : existing;
            int next = info.failures.incrementAndGet();
            if (next >= maxAttempts) {
                info.lockedUntil = Instant.now().plusSeconds(lockMinutes * 60);
            }
            return info;
        });
    }

    public void clear(String identity) {
        attempts.remove(identityKey(identity));
    }

    public long getActiveLockCount() {
        Instant now = Instant.now();
        return attempts.values().stream()
                .filter(i -> i.lockedUntil != null && i.lockedUntil.isAfter(now))
                .count();
    }

    private String identityKey(String identity) {
        return identity == null ? "unknown" : identity.trim().toLowerCase();
    }

    private static final class AttemptInfo {
        private final AtomicInteger failures = new AtomicInteger(0);
        private Instant lockedUntil;
    }
}

