package com.greenmiles.backend.driver.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record DriverApplicationRequest(
        @NotNull Long driverId,
        @NotBlank String fullName,
        @NotBlank String mobile,
        @NotBlank @Email String email,
        @NotBlank String vehicleModel,
        @NotBlank String vehicleRegistration,
        @NotBlank String carRcNumber,
        @NotBlank String driverLicenseNumber,
        @NotBlank String pollutionCertificateNumber,
        @NotBlank String roadTaxReceiptNumber,
        @NotBlank String permitNumber,
        @NotBlank String insurancePolicyNumber) {}
