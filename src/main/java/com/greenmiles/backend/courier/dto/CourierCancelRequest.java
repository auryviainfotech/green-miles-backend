package com.greenmiles.backend.courier.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CourierCancelRequest(@NotBlank @Size(max = 500) String reason) {}
