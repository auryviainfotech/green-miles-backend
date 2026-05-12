package com.greenmiles.backend.admin.dto;

public record AdminDriverResponse(
        Long driverId,
        String fullName,
        String phone,
        String licenseNumber,
        String email,
        String address,
        String vidProofNumber,
        String status,
        Double rating,
        Long carId,
        String vehicleNumber,
        String rcNumber,
        String vehicleType,
        Integer totalSeats,
        String carStatus) {}
