package com.greenmiles.backend.admin.dto;

import java.time.Instant;

public record RoutePlanResponse(
        Long routePlanId,
        String routeCode,
        Long startCityId,
        String startCityName,
        Long endCityId,
        String endCityName,
        Integer baseFare,
        Integer defaultSeats,
        Boolean active,
        Instant createdAt) {}
