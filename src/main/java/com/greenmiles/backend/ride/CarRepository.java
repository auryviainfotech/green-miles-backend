package com.greenmiles.backend.ride;

import java.util.Optional;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CarRepository extends JpaRepository<Car, Long> {
    Optional<Car> findFirstByDriverId(Long driverId);
    List<Car> findByDriverIdOrderByCarIdAsc(Long driverId);
    Optional<Car> findByCarIdAndDriverId(Long carId, Long driverId);
}
