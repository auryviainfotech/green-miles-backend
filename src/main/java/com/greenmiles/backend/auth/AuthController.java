package com.greenmiles.backend.auth;

import com.greenmiles.backend.auth.dto.AuthResponse;
import com.greenmiles.backend.auth.dto.AdminLoginRequest;
import com.greenmiles.backend.auth.dto.DriverLoginRequest;
import com.greenmiles.backend.auth.dto.LoginRequest;
import com.greenmiles.backend.auth.dto.RefreshTokenRequest;
import com.greenmiles.backend.auth.dto.RegisterRequest;
import com.greenmiles.backend.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("User registered", authService.register(request)));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Login successful", authService.login(request)));
    }

    @PostMapping("/driver/login")
    public ResponseEntity<ApiResponse<AuthResponse>> driverLogin(@Valid @RequestBody DriverLoginRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Driver login successful", authService.driverLogin(request)));
    }

    @PostMapping("/admin/login")
    public ResponseEntity<ApiResponse<AuthResponse>> adminLogin(@Valid @RequestBody AdminLoginRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Admin login successful", authService.adminLogin(request)));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Token refreshed", authService.refresh(request.refreshToken())));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader) {
        authService.logout(authHeader);
        return ResponseEntity.ok(ApiResponse.ok("Logout successful", null));
    }
}
