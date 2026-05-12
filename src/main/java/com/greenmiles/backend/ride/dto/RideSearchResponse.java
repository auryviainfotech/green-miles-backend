package com.greenmiles.backend.ride.dto;

import com.greenmiles.backend.ride.RideStatus;
import java.time.LocalDateTime;

public record RideSearchResponse(
        Long rideId,
        String startCity,
        String endCity,
        LocalDateTime startTime,
        RideStatus rideStatus,
        Integer availableSeats) {}
