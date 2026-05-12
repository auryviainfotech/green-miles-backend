package com.greenmiles.backend.courier.dto;

import jakarta.validation.constraints.NotBlank;

public record CourierStatusPatchRequest(@NotBlank String status) {}
