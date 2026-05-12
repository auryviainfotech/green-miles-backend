package com.greenmiles.backend.driver.dto;

import jakarta.validation.constraints.NotBlank;

public record DriverApplicationStatusUpdateRequest(@NotBlank String status) {}
