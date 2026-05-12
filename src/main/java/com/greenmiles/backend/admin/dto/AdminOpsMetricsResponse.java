package com.greenmiles.backend.admin.dto;

public record AdminOpsMetricsResponse(
        long driverCreateSuccess,
        long driverCreateFailure,
        long routeCreateSuccess,
        long routeCreateFailure,
        long rideAssignSuccess,
        long rideAssignFailure) {}

