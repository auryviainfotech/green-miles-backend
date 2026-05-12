package com.greenmiles.backend.audit;

import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public void logDeniedAccess(Long userId, String entity, Long entityId, String details) {
        AuditLog log = new AuditLog();
        log.setUserId(userId);
        log.setAction("ACCESS_DENIED");
        log.setEntity(entity);
        log.setEntityId(entityId);
        log.setDetails("{\"reason\":\"" + sanitize(details) + "\"}");
        log.setCreatedAt(Instant.now());
        auditLogRepository.save(log);
    }

    private String sanitize(String value) {
        return value == null ? "" : value.replace("\"", "'");
    }
}
