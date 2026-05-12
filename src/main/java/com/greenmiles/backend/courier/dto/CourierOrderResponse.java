package com.greenmiles.backend.courier.dto;

import java.time.Instant;
import java.time.LocalDateTime;

public record CourierOrderResponse(
        Long orderId,
        String awbNumber,
        String qrToken,
        Long userId,
        Long driverId,
        String driverName,
        String partnerVehicleHint,
        String pickup,
        String drop,
        double weight,
        double distanceKm,
        String packageCategory,
        String recipientName,
        String recipientPhone,
        String contentsNote,
        String pickupSlotLabel,
        int price,
        String status,
        int etaMins,
        String contactPhone,
        String contactEmail,
        Instant bookedAt,
        String cancelReason,
        Double lastLatitude,
        Double lastLongitude,
        LocalDateTime lastGpsRecordedAt) {}
