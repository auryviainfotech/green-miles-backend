package com.greenmiles.backend.auth.dto;

public record AuthResponse(Long userId, String fullName, String email, String token, String refreshToken) {}
