package com.greenmiles.backend.admin;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DriverProfileRepository extends JpaRepository<DriverProfile, Long> {
    List<DriverProfile> findByDriverDriverIdIn(List<Long> driverIds);
}
