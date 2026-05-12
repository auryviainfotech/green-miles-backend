package com.greenmiles.backend.driver.dto;

import java.time.Instant;

public record RideApplicationResponse(
        Long applicationId,
        Long driverId,
        String from,
        String to,
        String date,
        String time,
        Integer seats,
        Integer price,
        String status,
        Long approvedRideId,
        Instant submittedAt) {}
