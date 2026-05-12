package com.greenmiles.backend.admin.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RideAssignmentRequest(
        @NotNull Long routePlanId,
        @NotNull Long driverId,
        @NotNull Long carId,
        @NotBlank String startTime,
        @NotNull @Min(1) @Max(6) Integer availableSeats) {}
