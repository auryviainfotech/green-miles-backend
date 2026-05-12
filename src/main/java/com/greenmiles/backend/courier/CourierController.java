package com.greenmiles.backend.courier;

import com.greenmiles.backend.audit.AuditLogService;
import com.greenmiles.backend.common.ApiResponse;
import com.greenmiles.backend.courier.dto.CourierAssignRequest;
import com.greenmiles.backend.courier.dto.CourierCancelRequest;
import com.greenmiles.backend.courier.dto.CourierCreateOrderRequest;
import com.greenmiles.backend.courier.dto.CourierOrderResponse;
import com.greenmiles.backend.courier.dto.CourierStatusPatchRequest;
import com.greenmiles.backend.driver.dto.GpsPointResponse;
import com.greenmiles.backend.security.AuthContext;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/courier/orders")
public class CourierController {

    private final CourierOrderService courierOrderService;
    private final AuthContext authContext;
    private final AuditLogService auditLogService;

    public CourierController(
            CourierOrderService courierOrderService, AuthContext authContext, AuditLogService auditLogService) {
        this.courierOrderService = courierOrderService;
        this.authContext = authContext;
        this.auditLogService = auditLogService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CourierOrderResponse>> create(@Valid @RequestBody CourierCreateOrderRequest request) {
        enforceUserOwnership(request.userId());
        return ResponseEntity.ok(ApiResponse.ok("Courier order created", courierOrderService.create(request)));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<ApiResponse<CourierOrderResponse>> getById(@PathVariable Long orderId) {
        CourierOrderResponse order = courierOrderService.getById(orderId);
        enforceUserOrAdmin(order.userId());
        return ResponseEntity.ok(ApiResponse.ok("Courier order fetched", order));
    }

    @GetMapping("/{orderId}/tracking")
    public ResponseEntity<ApiResponse<List<GpsPointResponse>>> getTracking(@PathVariable Long orderId) {
        CourierOrderResponse order = courierOrderService.getById(orderId);
        enforceUserOrAdmin(order.userId());
        return ResponseEntity.ok(ApiResponse.ok("Courier tracking fetched", courierOrderService.getTracking(orderId)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CourierOrderResponse>>> list(@RequestParam(required = false) Long userId) {
        if (authContext.isAdmin()) {
            if (userId == null) {
                return ResponseEntity.ok(ApiResponse.ok("Courier orders fetched", courierOrderService.listAll()));
            }
            return ResponseEntity.ok(ApiResponse.ok("Courier orders fetched", courierOrderService.listForUser(userId)));
        }
        if (userId == null) {
            throw new IllegalArgumentException("userId is required");
        }
        enforceUserOwnership(userId);
        return ResponseEntity.ok(ApiResponse.ok("Courier orders fetched", courierOrderService.listForUser(userId)));
    }

    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<ApiResponse<CourierOrderResponse>> cancel(
            @PathVariable Long orderId, @Valid @RequestBody CourierCancelRequest request) {
        if (authContext.isAdmin()) {
            throw new AccessDeniedException("Admin should use status update for operational cancellation");
        }
        Long userId = authContext.currentUserId();
        CourierOrderResponse current = courierOrderService.getById(orderId);
        if (!current.userId().equals(userId)) {
            auditLogService.logDeniedAccess(
                    authContext.currentActorIdOrNull(),
                    CourierOrderService.ENTITY,
                    orderId,
                    "User attempted to cancel another user's courier");
            throw new AccessDeniedException("You are not allowed to cancel this order");
        }
        return ResponseEntity.ok(
                ApiResponse.ok("Courier order cancelled", courierOrderService.cancelByUser(orderId, userId, request.reason())));
    }

    @PatchMapping("/{orderId}/assign")
    public ResponseEntity<ApiResponse<CourierOrderResponse>> assign(
            @PathVariable Long orderId, @Valid @RequestBody CourierAssignRequest request) {
        if (!authContext.isAdmin()) {
            auditLogService.logDeniedAccess(
                    authContext.currentActorIdOrNull(),
                    CourierOrderService.ENTITY,
                    orderId,
                    "Non-admin attempted courier assignment");
            throw new AccessDeniedException("Admin role required");
        }
        return ResponseEntity.ok(
                ApiResponse.ok("Courier partner assigned", courierOrderService.assign(orderId, request.driverId())));
    }

    @PatchMapping("/{orderId}/status")
    public ResponseEntity<ApiResponse<CourierOrderResponse>> patchStatus(
            @PathVariable Long orderId, @Valid @RequestBody CourierStatusPatchRequest request) {
        if (!authContext.isAdmin()) {
            auditLogService.logDeniedAccess(
                    authContext.currentActorIdOrNull(),
                    CourierOrderService.ENTITY,
                    orderId,
                    "Non-admin attempted courier status update");
            throw new AccessDeniedException("Admin role required");
        }
        return ResponseEntity.ok(
                ApiResponse.ok("Courier status updated", courierOrderService.adminUpdateStatus(orderId, request.status())));
    }

    private void enforceUserOwnership(Long targetUserId) {
        if (authContext.isAdmin()) {
            return;
        }
        if (!targetUserId.equals(authContext.currentUserId())) {
            auditLogService.logDeniedAccess(
                    authContext.currentActorIdOrNull(),
                    CourierOrderService.ENTITY, null, "User ownership mismatch for userId=" + targetUserId);
            throw new AccessDeniedException("You are not allowed to access this user resource");
        }
    }

    private void enforceUserOrAdmin(Long ownerUserId) {
        if (authContext.isAdmin()) {
            return;
        }
        enforceUserOwnership(ownerUserId);
    }
}
