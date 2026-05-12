package com.greenmiles.backend.admin;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentLogRepository extends JpaRepository<PaymentLog, Long> {
    long countByStatus(String status);

    @Query("select coalesce(sum(p.amount), 0) from PaymentLog p where p.status = :status")
    long sumAmountByStatus(String status);
}
