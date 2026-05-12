package com.greenmiles.backend.booking;

import com.greenmiles.backend.booking.dto.BookingResponse;
import com.greenmiles.backend.booking.dto.CancelBookingRequest;
import com.greenmiles.backend.booking.dto.CreateBookingRequest;
import com.greenmiles.backend.audit.AuditLogService;
import com.greenmiles.backend.common.ApiResponse;
import com.greenmiles.backend.security.AuthContext;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/bookings")
public class BookingController {

    private final BookingService bookingService;
    private final AuthContext authContext;
    private final AuditLogService auditLogService;

    public BookingController(BookingService bookingService, AuthContext authContext, AuditLogService auditLogService) {
        this.bookingService = bookingService;
        this.authContext = authContext;
        this.auditLogService = auditLogService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<BookingResponse>> create(@Valid @RequestBody CreateBookingRequest request) {
        enforceUserOwnership(request.userId());
        return ResponseEntity.ok(ApiResponse.ok("Booking created", bookingService.create(request)));
    }

    @GetMapping("/{bookingId}")
    public ResponseEntity<ApiResponse<BookingResponse>> getById(@PathVariable Long bookingId) {
        BookingResponse booking = bookingService.getById(bookingId);
        enforceUserOwnership(booking.userId());
        return ResponseEntity.ok(ApiResponse.ok("Booking fetched", booking));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<BookingResponse>>> getByUser(@RequestParam Long userId) {
        enforceUserOwnership(userId);
        return ResponseEntity.ok(ApiResponse.ok("User bookings fetched", bookingService.getUserBookings(userId)));
    }

    @PostMapping("/{bookingId}/cancel")
    public ResponseEntity<ApiResponse<BookingResponse>> cancelByUser(
            @PathVariable Long bookingId, @Valid @RequestBody CancelBookingRequest request) {
        BookingResponse booking = bookingService.getById(bookingId);
        enforceUserOwnership(booking.userId());
        return ResponseEntity.ok(ApiResponse.ok(
                "Booking cancelled", bookingService.cancel(bookingId, request.reason(), CancelledBy.USER)));
    }

    private void enforceUserOwnership(Long targetUserId) {
        if (authContext.isAdmin()) {
            return;
        }
        if (!targetUserId.equals(authContext.currentUserId())) {
            auditLogService.logDeniedAccess(
                    authContext.currentActorIdOrNull(), "BOOKING", null, "User ownership mismatch for userId=" + targetUserId);
            throw new AccessDeniedException("You are not allowed to access this user resource");
        }
    }
}
