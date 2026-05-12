package com.greenmiles.backend.driver;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DriverRepository extends JpaRepository<Driver, Long> {
    Optional<Driver> findByDriverIdAndPhone(Long driverId, String phone);
    boolean existsByPhone(String phone);
    boolean existsByLicenseNumber(String licenseNumber);
}
