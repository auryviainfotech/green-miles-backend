package com.greenmiles.backend.admin.dto;

import java.time.Instant;

public record AdminAuditLogResponse(
        Long logId, Long userId, String action, String entity, Long entityId, String details, Instant createdAt) {}
