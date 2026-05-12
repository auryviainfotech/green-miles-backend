package com.greenmiles.backend.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.greenmiles.backend.auth.dto.AuthResponse;
import com.greenmiles.backend.auth.dto.LoginRequest;
import com.greenmiles.backend.auth.dto.RegisterRequest;
import com.greenmiles.backend.audit.AuditLogRepository;
import com.greenmiles.backend.driver.DriverRepository;
import com.greenmiles.backend.security.JwtService;
import com.greenmiles.backend.security.LoginAttemptService;
import com.greenmiles.backend.security.SecurityMetricsService;
import com.greenmiles.backend.user.AccountStatus;
import com.greenmiles.backend.user.User;
import com.greenmiles.backend.user.UserRepository;
import java.lang.reflect.Field;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private DriverRepository driverRepository;

    @Mock
    private SessionRepository sessionRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private JwtService jwtService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private LoginAttemptService loginAttemptService;

    @Mock
    private SecurityMetricsService securityMetricsService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                userRepository,
                driverRepository,
                sessionRepository,
                refreshTokenRepository,
                auditLogRepository,
                jwtService,
                passwordEncoder,
                loginAttemptService,
                securityMetricsService,
                120L,
                "admin",
                "admin@123");
    }

    @Test
    void registerStoresEncodedPassword() {
        RegisterRequest request = new RegisterRequest("Test User", "9876543210", "test@greenmiles.in", "Password@123");
        User savedUser = new User();
        savedUser.setFullName("Test User");
        savedUser.setEmail("test@greenmiles.in");
        savedUser.setPhone("9876543210");
        savedUser.setPasswordHash("$2a$10$encodedValue");
        savedUser.setAccountStatus(AccountStatus.ACTIVE);
        setPrivateField(savedUser, "userId", 21L);

        when(userRepository.existsByEmail("test@greenmiles.in")).thenReturn(false);
        when(userRepository.existsByPhone("9876543210")).thenReturn(false);
        when(passwordEncoder.encode("Password@123")).thenReturn("$2a$10$encodedValue");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtService.generateToken("USER:21", "USER", "test@greenmiles.in")).thenReturn("jwt-token");
        when(jwtService.generateRefreshToken("USER:21", "test@greenmiles.in")).thenReturn("refresh-token");
        when(jwtService.getRefreshExpirationMinutes()).thenReturn(10080L);

        AuthResponse response = authService.register(request);

        assertEquals(21L, response.userId());
        assertEquals("jwt-token", response.token());
        assertEquals("refresh-token", response.refreshToken());
        verify(passwordEncoder).encode("Password@123");
        verify(sessionRepository).save(any(Session.class));
    }

    @Test
    void loginAcceptsEncodedPasswordWithoutLegacyMigration() {
        User user = buildUser(5L, "real@greenmiles.in", "$2a$10$validHash");

        when(userRepository.findByEmail("real@greenmiles.in")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Password@123", "$2a$10$validHash")).thenReturn(true);
        when(jwtService.generateToken("USER:5", "USER", "real@greenmiles.in")).thenReturn("jwt-user");
        when(jwtService.generateRefreshToken("USER:5", "real@greenmiles.in")).thenReturn("refresh-user");
        when(jwtService.getRefreshExpirationMinutes()).thenReturn(10080L);

        AuthResponse response = authService.login(new LoginRequest("real@greenmiles.in", "Password@123"));

        assertEquals(5L, response.userId());
        assertEquals("jwt-user", response.token());
        assertEquals("refresh-user", response.refreshToken());
        verify(userRepository, never()).save(user);
        verify(sessionRepository).save(any(Session.class));
    }

    @Test
    void loginMigratesLegacyPlainTextPassword() {
        User user = buildUser(9L, "legacy@greenmiles.in", "legacy-pass");

        when(userRepository.findByEmail("legacy@greenmiles.in")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("legacy-pass", "legacy-pass")).thenReturn(false);
        when(passwordEncoder.encode("legacy-pass")).thenReturn("$2a$10$newHash");
        when(jwtService.generateToken("USER:9", "USER", "legacy@greenmiles.in")).thenReturn("jwt-legacy");
        when(jwtService.generateRefreshToken("USER:9", "legacy@greenmiles.in")).thenReturn("refresh-legacy");
        when(jwtService.getRefreshExpirationMinutes()).thenReturn(10080L);

        AuthResponse response = authService.login(new LoginRequest("legacy@greenmiles.in", "legacy-pass"));

        assertEquals(9L, response.userId());
        assertEquals("jwt-legacy", response.token());
        assertEquals("refresh-legacy", response.refreshToken());
        assertEquals("$2a$10$newHash", user.getPasswordHash());
        verify(userRepository).save(user);
        verify(sessionRepository).save(any(Session.class));
    }

    @Test
    void loginRejectsWrongPassword() {
        User user = buildUser(3L, "wrong@greenmiles.in", "$2a$10$hash");
        when(userRepository.findByEmail("wrong@greenmiles.in")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("bad-pass", "$2a$10$hash")).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> authService.login(new LoginRequest("wrong@greenmiles.in", "bad-pass")));
        verify(sessionRepository, never()).save(any(Session.class));
    }

    @Test
    void logoutDeletesTokenFromSessionStore() {
        authService.logout("Bearer token-123");
        verify(sessionRepository).deleteByToken("token-123");
        verify(refreshTokenRepository).deleteByToken("token-123");
    }

    private User buildUser(Long id, String email, String passwordHash) {
        User user = new User();
        user.setFullName("User");
        user.setEmail(email);
        user.setPhone("9999999999");
        user.setPasswordHash(passwordHash);
        user.setAccountStatus(AccountStatus.ACTIVE);
        setPrivateField(user, "userId", id);
        return user;
    }

    private void setPrivateField(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception ex) {
            throw new AssertionError("Unable to set test field: " + fieldName, ex);
        }
    }
}

