package com.greenmiles.backend.courier.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CourierCreateOrderRequest(
        @NotNull Long userId,
        @NotBlank String pickup,
        @NotBlank String drop,
        @Min(1) @Max(80) double weight,
        @Positive double distanceKm,
        @NotBlank @Pattern(regexp = "\\d{10}") String contactPhone,
        @NotBlank @Email String contactEmail,
        @Positive int price,
        @Size(max = 32) String packageCategory,
        @Size(max = 200) String recipientName,
        @Size(max = 20) String recipientPhone,
        @Size(max = 500) String contentsNote,
        @Size(max = 120) String pickupSlotLabel) {}
