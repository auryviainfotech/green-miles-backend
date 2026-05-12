package com.greenmiles.backend.driver;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GpsTrackingRepository extends JpaRepository<GpsTracking, Long> {
    List<GpsTracking> findTop20ByDriverDriverIdOrderByRecordedAtDesc(Long driverId);

    List<GpsTracking> findTop50ByCourierOrder_CourierOrderIdOrderByRecordedAtDesc(Long courierOrderId);

    Optional<GpsTracking> findFirstByCourierOrder_CourierOrderIdOrderByRecordedAtDesc(Long courierOrderId);
}
