package com.greenmiles.backend.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    Page<AuditLog> findByActionOrderByCreatedAtDesc(String action, Pageable pageable);

    Page<AuditLog> findByActionAndEntityOrderByCreatedAtDesc(String action, String entity, Pageable pageable);

    List<AuditLog> findByEntityOrderByCreatedAtDesc(String entity);
}
