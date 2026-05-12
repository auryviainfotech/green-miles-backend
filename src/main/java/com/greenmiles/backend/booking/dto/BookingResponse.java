package com.greenmiles.backend.booking.dto;

import com.greenmiles.backend.booking.BookingStatus;
import java.time.Instant;

public record BookingResponse(
        Long bookingId,
        Long userId,
        Long rideId,
        Integer seatNumber,
        BookingStatus status,
        Instant bookingTime,
        String rideStatus) {}
