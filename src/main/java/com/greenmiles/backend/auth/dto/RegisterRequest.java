package com.greenmiles.backend.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Size(min = 2, max = 120) String fullName,
        @NotBlank @Pattern(regexp = "^[0-9]{10,15}$") String phone,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, max = 64) String password) {}
