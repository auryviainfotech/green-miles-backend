package com.greenmiles.backend.booking.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CreateBookingRequest(
        @NotNull Long userId,
        @NotNull Long rideId,
        @NotNull Long pickupPointId,
        @NotNull Long dropPointId,
        @NotNull @Min(1) @Max(6) Integer seatNumber,
        String paymentMethod) {}
