package com.greenmiles.backend.admin.dto;

import java.time.LocalDateTime;

public record RideAssignmentResponse(
        Long rideId,
        Long routePlanId,
        String routeCode,
        Long driverId,
        String driverName,
        Long carId,
        String vehicleNumber,
        String startCity,
        String endCity,
        LocalDateTime startTime,
        String rideStatus,
        Integer availableSeats) {}
