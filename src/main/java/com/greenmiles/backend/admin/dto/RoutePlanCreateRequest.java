package com.greenmiles.backend.admin.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record RoutePlanCreateRequest(
        String routeCode,
        @NotNull Long startCityId,
        @NotNull Long endCityId,
        @NotNull @Min(50) Integer baseFare,
        @NotNull @Min(1) @Max(12) Integer defaultSeats) {}
