package com.greenmiles.backend.driver.dto;

import com.greenmiles.backend.ride.RideStatus;
import java.time.LocalDateTime;

public record DriverRideResponse(Long rideId, Long driverId, RideStatus status, LocalDateTime startTime, Integer availableSeats) {}
