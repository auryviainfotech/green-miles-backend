package com.greenmiles.backend.driver.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RideApplicationRequest(
        @NotNull Long driverId,
        @NotBlank String from,
        @NotBlank String to,
        @NotBlank String date,
        @NotBlank String time,
        @NotNull @Min(1) @Max(12) Integer seats,
        @NotNull @Min(50) Integer price) {}
