package com.greenmiles.backend.admin.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AdminCreateDriverRequest(
        @NotBlank String fullName,
        @NotBlank String phone,
        @Email String email,
        String address,
        String vidProofNumber,
        @NotBlank String licenseNumber,
        @NotBlank String status,
        @NotNull @Min(1) @Max(5) Double rating,
        @NotBlank String vehicleNumber,
        @NotBlank String rcNumber,
        @NotBlank String vehicleType,
        @NotNull @Min(2) @Max(12) Integer totalSeats,
        @NotBlank String carStatus) {}
