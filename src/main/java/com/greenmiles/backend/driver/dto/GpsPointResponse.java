package com.greenmiles.backend.driver.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record GpsPointResponse(
        Long gpsId,
        Long driverId,
        Long rideId,
        Long courierOrderId,
        BigDecimal latitude,
        BigDecimal longitude,
        BigDecimal speed,
        Integer heading,
        LocalDateTime recordedAt) {}
