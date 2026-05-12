package com.greenmiles.backend.admin;

import com.greenmiles.backend.admin.dto.AdminAuditLogPageResponse;
import com.greenmiles.backend.admin.dto.AdminCreateDriverRequest;
import com.greenmiles.backend.admin.dto.AdminDashboardResponse;
import com.greenmiles.backend.admin.dto.AdminDriverResponse;
import com.greenmiles.backend.admin.dto.AdminOpsMetricsResponse;
import com.greenmiles.backend.admin.dto.PaymentLogResponse;
import com.greenmiles.backend.admin.dto.RideAssignmentRequest;
import com.greenmiles.backend.admin.dto.RideAssignmentResponse;
import com.greenmiles.backend.admin.dto.RoutePlanCreateRequest;
import com.greenmiles.backend.admin.dto.RoutePlanResponse;
import com.greenmiles.backend.common.ApiResponse;
import com.greenmiles.backend.security.AuthContext;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    private final AdminService adminService;
    private final AuthContext authContext;

    public AdminController(AdminService adminService, AuthContext authContext) {
        this.adminService = adminService;
        this.authContext = authContext;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<AdminDashboardResponse>> dashboard() {
        ensureAdmin();
        return ResponseEntity.ok(ApiResponse.ok("Admin dashboard fetched", adminService.getDashboard()));
    }

    @GetMapping("/drivers")
    public ResponseEntity<ApiResponse<List<AdminDriverResponse>>> listDrivers() {
        ensureAdmin();
        return ResponseEntity.ok(ApiResponse.ok("Drivers fetched", adminService.listDrivers()));
    }

    @PostMapping("/drivers")
    public ResponseEntity<ApiResponse<AdminDriverResponse>> createDriver(
            @Valid @RequestBody AdminCreateDriverRequest request) {
        ensureAdmin();
        return ResponseEntity.ok(ApiResponse.ok("Driver created", adminService.createDriver(request)));
    }

    @GetMapping("/routes")
    public ResponseEntity<ApiResponse<List<RoutePlanResponse>>> listRoutes() {
        ensureAdmin();
        return ResponseEntity.ok(ApiResponse.ok("Route plans fetched", adminService.listRoutes()));
    }

    @PostMapping("/routes")
    public ResponseEntity<ApiResponse<RoutePlanResponse>> createRoute(
            @Valid @RequestBody RoutePlanCreateRequest request) {
        ensureAdmin();
        return ResponseEntity.ok(ApiResponse.ok("Route plan created", adminService.createRoute(request)));
    }

    @PostMapping("/ride-assignments")
    public ResponseEntity<ApiResponse<RideAssignmentResponse>> assignRide(
            @Valid @RequestBody RideAssignmentRequest request) {
        ensureAdmin();
        return ResponseEntity.ok(ApiResponse.ok("Ride assigned to driver", adminService.assignRide(request)));
    }

    @GetMapping("/ride-assignments")
    public ResponseEntity<ApiResponse<List<RideAssignmentResponse>>> listRideAssignments() {
        ensureAdmin();
        return ResponseEntity.ok(ApiResponse.ok("Ride assignments fetched", adminService.listRideAssignments()));
    }

    @GetMapping("/payments")
    public ResponseEntity<ApiResponse<List<PaymentLogResponse>>> payments() {
        ensureAdmin();
        return ResponseEntity.ok(ApiResponse.ok("Payment logs fetched", adminService.listPaymentLogs()));
    }

    @GetMapping("/audit-logs")
    public ResponseEntity<ApiResponse<AdminAuditLogPageResponse>> deniedAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String entity) {
        ensureAdmin();
        return ResponseEntity.ok(
                ApiResponse.ok("Denied access audit logs fetched", adminService.getDeniedAuditLogs(page, size, entity)));
    }

    @GetMapping("/ops-metrics")
    public ResponseEntity<ApiResponse<AdminOpsMetricsResponse>> opsMetrics() {
        ensureAdmin();
        return ResponseEntity.ok(ApiResponse.ok("Admin ops metrics fetched", adminService.getOpsMetrics()));
    }

    private void ensureAdmin() {
        if (!authContext.isAdmin()) {
            throw new AccessDeniedException("Admin access required");
        }
    }
}
