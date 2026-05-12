package com.greenmiles.backend.courier.dto;

import jakarta.validation.constraints.NotNull;

public record CourierAssignRequest(@NotNull Long driverId) {}
