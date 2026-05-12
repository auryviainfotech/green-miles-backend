package com.greenmiles.backend.auth;

import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SessionRepository extends JpaRepository<Session, Long> {
    boolean existsByTokenAndExpiryTimeAfter(String token, LocalDateTime now);

    void deleteByToken(String token);

    void deleteByExpiryTimeBefore(LocalDateTime now);
}
