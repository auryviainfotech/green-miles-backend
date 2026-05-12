package com.greenmiles.backend.admin.dto;

import java.time.Instant;

public record PaymentLogResponse(
        Long paymentLogId,
        Long bookingId,
        Long userId,
        Long rideId,
        Integer amount,
        String status,
        String method,
        String referenceCode,
        Instant createdAt) {}
