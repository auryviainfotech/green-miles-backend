package com.greenmiles.backend.ride;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RideRepository extends JpaRepository<Ride, Long> {

    @Query("select r from Ride r where r.startCity.cityId = :startCityId and r.endCity.cityId = :endCityId and r.rideStatus in :statuses and r.startTime between :from and :to order by r.startTime asc")
    List<Ride> searchRides(
            @Param("startCityId") Long startCityId,
            @Param("endCityId") Long endCityId,
            @Param("statuses") List<RideStatus> statuses,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);
}
