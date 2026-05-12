package com.greenmiles.backend.admin;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoutePlanRepository extends JpaRepository<RoutePlan, Long> {
    List<RoutePlan> findByActiveTrueOrderByCreatedAtDesc();
}
