package com.greenmiles.backend.driver.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record GpsUpdateRequest(
        @NotNull Long driverId,
        Long rideId,
        Long courierOrderId,
        @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") BigDecimal latitude,
        @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") BigDecimal longitude,
        BigDecimal speed,
        Integer heading) {

    @AssertTrue(message = "Provide either rideId or courierOrderId, not both")
    public boolean isValidReference() {
        return rideId == null || courierOrderId == null;
    }
}
