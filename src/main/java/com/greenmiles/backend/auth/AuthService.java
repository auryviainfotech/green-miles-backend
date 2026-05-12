package com.greenmiles.backend.auth;

import com.greenmiles.backend.auth.dto.AuthResponse;
import com.greenmiles.backend.auth.dto.AdminLoginRequest;
import com.greenmiles.backend.auth.dto.DriverLoginRequest;
import com.greenmiles.backend.auth.dto.LoginRequest;
import com.greenmiles.backend.auth.dto.RegisterRequest;
import com.greenmiles.backend.audit.AuditLog;
import com.greenmiles.backend.audit.AuditLogRepository;
import com.greenmiles.backend.driver.Driver;
import com.greenmiles.backend.driver.DriverRepository;
import com.greenmiles.backend.security.JwtService;
import com.greenmiles.backend.security.LoginAttemptService;
import com.greenmiles.backend.security.SecurityMetricsService;
import com.greenmiles.backend.user.AccountStatus;
import com.greenmiles.backend.user.User;
import com.greenmiles.backend.user.UserRepository;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private static final Pattern DRIVER_EMAIL_PATTERN = Pattern.compile("^driver(\\d+)(?:@.*)?$", Pattern.CASE_INSENSITIVE);

    private final UserRepository userRepository;
    private final DriverRepository driverRepository;
    private final SessionRepository sessionRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AuditLogRepository auditLogRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final LoginAttemptService loginAttemptService;
    private final SecurityMetricsService securityMetricsService;
    private final long jwtExpirationMinutes;
    private final String adminUsername;
    private final String adminPassword;

    public AuthService(
            UserRepository userRepository,
            DriverRepository driverRepository,
            SessionRepository sessionRepository,
            RefreshTokenRepository refreshTokenRepository,
            AuditLogRepository auditLogRepository,
            JwtService jwtService,
            PasswordEncoder passwordEncoder,
            LoginAttemptService loginAttemptService,
            SecurityMetricsService securityMetricsService,
            @Value("${app.security.jwt.expiration-minutes}") long jwtExpirationMinutes,
            @Value("${app.security.admin.username}") String adminUsername,
            @Value("${app.security.admin.password}") String adminPassword) {
        this.userRepository = userRepository;
        this.driverRepository = driverRepository;
        this.sessionRepository = sessionRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.auditLogRepository = auditLogRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.loginAttemptService = loginAttemptService;
        this.securityMetricsService = securityMetricsService;
        this.jwtExpirationMinutes = jwtExpirationMinutes;
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email already registered");
        }
        if (userRepository.existsByPhone(request.phone())) {
            throw new IllegalArgumentException("Phone already registered");
        }

        User user = new User();
        user.setFullName(request.fullName().trim());
        user.setEmail(request.email().trim().toLowerCase());
        user.setPhone(request.phone().trim());
        user.setPasswordHash(passwordEncoder.encode(request.password().trim()));
        user.setAccountStatus(AccountStatus.ACTIVE);
        user.setCreatedAt(Instant.now());
        User saved = userRepository.save(user);

        String token = jwtService.generateToken("USER:" + saved.getUserId(), "USER", saved.getEmail());
        String refreshToken = createRefreshToken(saved);
        saveSession(saved, token);
        return new AuthResponse(saved.getUserId(), saved.getFullName(), saved.getEmail(), token, refreshToken);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        String rawEmail = request.email().trim();
        String normalizedEmail = rawEmail.toLowerCase();
        String normalizedPassword = request.password().trim();

        if (loginAttemptService.isLocked(rawEmail)) {
            securityMetricsService.incrementLockoutBlocks();
            throw new IllegalArgumentException("Too many failed attempts. Try again later.");
        }

        if (isAdminLoginIdentifier(rawEmail) && adminPassword.trim().equals(normalizedPassword)) {
            String token = jwtService.generateToken("ADMIN:" + adminUsername, "ADMIN", adminUsername);
            loginAttemptService.clear(rawEmail);
            return new AuthResponse(0L, "System Admin", adminUsername, token, null);
        }

        User user = userRepository.findByEmail(normalizedEmail).orElse(null);
        if (user != null) {
            if (!matchesPassword(normalizedPassword, user.getPasswordHash())) {
                loginAttemptService.recordFailure(normalizedEmail);
                auditAuthFailure("USER_LOGIN_FAILED", normalizedEmail, "Password mismatch");
                throw new IllegalArgumentException("Invalid credentials");
            }
            migrateLegacyPasswordIfNeeded(user, normalizedPassword);
            if (user.getAccountStatus() != AccountStatus.ACTIVE) {
                loginAttemptService.recordFailure(normalizedEmail);
                auditAuthFailure("USER_LOGIN_FAILED", normalizedEmail, "Account is not active");
                throw new IllegalArgumentException("Account is not active");
            }
            String token = jwtService.generateToken("USER:" + user.getUserId(), "USER", user.getEmail());
            String refreshToken = createRefreshToken(user);
            saveSession(user, token);
            loginAttemptService.clear(normalizedEmail);
            return new AuthResponse(user.getUserId(), user.getFullName(), user.getEmail(), token, refreshToken);
        }

        Long driverId = extractDriverIdFromIdentifier(rawEmail);
        if (driverId != null) {
            Driver driver = driverRepository
                    .findByDriverIdAndPhone(driverId, normalizedPassword)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));
            String token = jwtService.generateToken("DRIVER:" + driver.getDriverId(), "DRIVER", "driver-" + driver.getDriverId());
            loginAttemptService.clear(rawEmail);
            return new AuthResponse(driver.getDriverId(), driver.getFullName(), "driver-" + driver.getDriverId(), token, null);
        }

        loginAttemptService.recordFailure(rawEmail);
        auditAuthFailure("LOGIN_FAILED", rawEmail, "No matching identity");
        throw new IllegalArgumentException("Invalid credentials");
    }

    @Transactional(readOnly = true)
    public AuthResponse driverLogin(DriverLoginRequest request) {
        String identity = "driver:" + request.driverId();
        if (loginAttemptService.isLocked(identity)) {
            securityMetricsService.incrementLockoutBlocks();
            throw new IllegalArgumentException("Too many failed attempts. Try again later.");
        }
        Driver driver = driverRepository
                .findByDriverIdAndPhone(request.driverId(), request.phone().trim())
                .orElseThrow(() -> {
                    loginAttemptService.recordFailure(identity);
                    auditAuthFailure("DRIVER_LOGIN_FAILED", identity, "Invalid driver credentials");
                    return new IllegalArgumentException("Invalid driver credentials");
                });
        String token = jwtService.generateToken("DRIVER:" + driver.getDriverId(), "DRIVER", "driver-" + driver.getDriverId());
        loginAttemptService.clear(identity);
        return new AuthResponse(driver.getDriverId(), driver.getFullName(), "driver-" + driver.getDriverId(), token, null);
    }

    @Transactional(readOnly = true)
    public AuthResponse adminLogin(AdminLoginRequest request) {
        String identity = "admin:" + request.username();
        if (loginAttemptService.isLocked(identity)) {
            securityMetricsService.incrementLockoutBlocks();
            throw new IllegalArgumentException("Too many failed attempts. Try again later.");
        }
        if (!adminUsername.equals(request.username().trim()) || !adminPassword.trim().equals(request.password().trim())) {
            loginAttemptService.recordFailure(identity);
            auditAuthFailure("ADMIN_LOGIN_FAILED", request.username(), "Invalid admin credentials");
            throw new IllegalArgumentException("Invalid admin credentials");
        }
        String token = jwtService.generateToken("ADMIN:" + adminUsername, "ADMIN", adminUsername);
        loginAttemptService.clear(identity);
        return new AuthResponse(0L, "System Admin", adminUsername, token, null);
    }

    @Transactional
    public void logout(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Authorization token is required");
        }
        String token = authHeader.substring(7).trim();
        if (token.isBlank()) {
            throw new IllegalArgumentException("Authorization token is required");
        }
        sessionRepository.deleteByToken(token);
        refreshTokenRepository.deleteByToken(token);
    }

    @Transactional
    public AuthResponse refresh(String rawRefreshToken) {
        String refreshToken = rawRefreshToken.trim();
        if (refreshToken.isBlank()) {
            throw new IllegalArgumentException("Refresh token is required");
        }
        RefreshToken stored = refreshTokenRepository
                .findByToken(refreshToken)
                .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));
        if (stored.getExpiryTime().isBefore(LocalDateTime.now())) {
            refreshTokenRepository.delete(stored);
            throw new IllegalArgumentException("Refresh token expired");
        }

        User user = stored.getUser();
        String newAccessToken = jwtService.generateToken("USER:" + user.getUserId(), "USER", user.getEmail());
        String newRefreshToken = jwtService.generateRefreshToken("USER:" + user.getUserId(), user.getEmail());

        refreshTokenRepository.delete(stored);
        RefreshToken replacement = new RefreshToken();
        replacement.setUser(user);
        replacement.setToken(newRefreshToken);
        replacement.setCreatedAt(Instant.now());
        replacement.setExpiryTime(LocalDateTime.now().plusMinutes(jwtService.getRefreshExpirationMinutes()));
        refreshTokenRepository.save(replacement);

        saveSession(user, newAccessToken);
        return new AuthResponse(user.getUserId(), user.getFullName(), user.getEmail(), newAccessToken, newRefreshToken);
    }

    private void saveSession(User user, String token) {
        Session session = new Session();
        session.setUser(user);
        session.setToken(token);
        session.setCreatedAt(Instant.now());
        session.setExpiryTime(LocalDateTime.now().plusMinutes(jwtExpirationMinutes));
        sessionRepository.save(session);
    }

    private boolean isAdminLoginIdentifier(String emailOrUsername) {
        if (emailOrUsername == null || emailOrUsername.isBlank()) {
            return false;
        }
        String value = emailOrUsername.trim();
        if (adminUsername.equals(value)) {
            return true;
        }
        if (value.contains("@")) {
            String localPart = value.split("@", 2)[0];
            return adminUsername.equals(localPart);
        }
        return false;
    }

    private Long extractDriverIdFromIdentifier(String identifier) {
        if (identifier == null || identifier.isBlank()) return null;
        Matcher matcher = DRIVER_EMAIL_PATTERN.matcher(identifier.trim());
        if (!matcher.matches()) return null;
        try {
            return Long.parseLong(matcher.group(1));
        } catch (Exception ex) {
            return null;
        }
    }

    private boolean matchesPassword(String rawPassword, String storedPasswordHash) {
        try {
            if (passwordEncoder.matches(rawPassword, storedPasswordHash)) {
                return true;
            }
        } catch (Exception ignored) {
            // Legacy plain-text values may not look like encoded hashes.
        }
        return rawPassword.equals(storedPasswordHash);
    }

    private void migrateLegacyPasswordIfNeeded(User user, String rawPassword) {
        String stored = user.getPasswordHash();
        if (stored == null || stored.isBlank() || stored.equals(rawPassword)) {
            user.setPasswordHash(passwordEncoder.encode(rawPassword));
            userRepository.save(user);
        }
    }

    private void auditAuthFailure(String action, String entityId, String reason) {
        securityMetricsService.incrementAuthFailures();
        AuditLog log = new AuditLog();
        log.setUserId(null);
        log.setAction(action);
        log.setEntity("AUTH");
        log.setEntityId(null);
        String safeEntityId = entityId == null ? "unknown" : entityId.trim();
        log.setDetails("{\"identity\":\"" + safeEntityId.replace("\"", "\\\"") + "\",\"reason\":\""
                + reason.replace("\"", "\\\"") + "\"}");
        log.setCreatedAt(Instant.now());
        auditLogRepository.save(log);
    }

    private String createRefreshToken(User user) {
        String refreshToken = jwtService.generateRefreshToken("USER:" + user.getUserId(), user.getEmail());
        RefreshToken tokenRecord = new RefreshToken();
        tokenRecord.setUser(user);
        tokenRecord.setToken(refreshToken);
        tokenRecord.setCreatedAt(Instant.now());
        tokenRecord.setExpiryTime(LocalDateTime.now().plusMinutes(jwtService.getRefreshExpirationMinutes()));
        refreshTokenRepository.save(tokenRecord);
        return refreshToken;
    }
}
