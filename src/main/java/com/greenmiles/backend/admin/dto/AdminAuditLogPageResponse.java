package com.greenmiles.backend.admin.dto;

import java.util.List;

public record AdminAuditLogPageResponse(
        List<AdminAuditLogResponse> items, int page, int size, long totalItems, int totalPages) {}
