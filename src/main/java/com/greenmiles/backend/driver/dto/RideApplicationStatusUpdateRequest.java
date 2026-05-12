package com.greenmiles.backend.driver.dto;

import jakarta.validation.constraints.NotBlank;

public record RideApplicationStatusUpdateRequest(@NotBlank String status) {}
