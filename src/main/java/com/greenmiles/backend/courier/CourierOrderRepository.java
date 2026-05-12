package com.greenmiles.backend.courier;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourierOrderRepository extends JpaRepository<CourierOrder, Long> {

    List<CourierOrder> findAllByOrderByCreatedAtDesc();

    List<CourierOrder> findByUser_UserIdOrderByCreatedAtDesc(Long userId);

    List<CourierOrder> findByDriver_DriverIdOrderByCreatedAtDesc(Long driverId);

    boolean existsByAwbNumber(String awbNumber);
}
