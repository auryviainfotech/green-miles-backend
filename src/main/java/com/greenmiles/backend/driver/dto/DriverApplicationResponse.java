package com.greenmiles.backend.driver.dto;

import java.time.Instant;

public record DriverApplicationResponse(
        Long applicationId,
        Long driverId,
        String fullName,
        String email,
        String mobile,
        String vehicleModel,
        String vehicleRegistration,
        String status,
        String reviewedBy,
        Instant submittedAt) {}
