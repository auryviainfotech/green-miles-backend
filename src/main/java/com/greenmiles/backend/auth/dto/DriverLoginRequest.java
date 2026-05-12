package com.greenmiles.backend.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record DriverLoginRequest(@NotNull Long driverId, @NotBlank String phone) {}
