package com.greenmiles.backend.driver;

import com.greenmiles.backend.audit.AuditLogService;
import com.greenmiles.backend.common.ApiResponse;
import com.greenmiles.backend.courier.CourierOrderService;
import com.greenmiles.backend.courier.dto.CourierOrderResponse;
import com.greenmiles.backend.courier.dto.CourierStatusPatchRequest;
import com.greenmiles.backend.driver.dto.DriverApplicationRequest;
import com.greenmiles.backend.driver.dto.DriverApplicationResponse;
import com.greenmiles.backend.driver.dto.DriverApplicationStatusUpdateRequest;
import com.greenmiles.backend.driver.dto.DriverRideResponse;
import com.greenmiles.backend.driver.dto.GpsPointResponse;
import com.greenmiles.backend.driver.dto.GpsUpdateRequest;
import com.greenmiles.backend.driver.dto.RideApplicationRequest;
import com.greenmiles.backend.driver.dto.RideApplicationResponse;
import com.greenmiles.backend.driver.dto.RideApplicationStatusUpdateRequest;
import com.greenmiles.backend.ride.RideStatus;
import com.greenmiles.backend.security.AuthContext;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/drivers")
public class DriverController {

    private final DriverService driverService;
    private final DriverApplicationService driverApplicationService;
    private final RideApplicationService rideApplicationService;
    private final CourierOrderService courierOrderService;
    private final AuthContext authContext;
    private final AuditLogService auditLogService;

    public DriverController(
            DriverService driverService,
            DriverApplicationService driverApplicationService,
            RideApplicationService rideApplicationService,
            CourierOrderService courierOrderService,
            AuthContext authContext,
            AuditLogService auditLogService) {
        this.driverService = driverService;
        this.driverApplicationService = driverApplicationService;
        this.rideApplicationService = rideApplicationService;
        this.courierOrderService = courierOrderService;
        this.authContext = authContext;
        this.auditLogService = auditLogService;
    }

    @PostMapping("/applications")
    public ResponseEntity<ApiResponse<DriverApplicationResponse>> submitApplication(
            @Valid @RequestBody DriverApplicationRequest request) {
        enforceDriverOwnership(request.driverId());
        return ResponseEntity.ok(
                ApiResponse.ok("Driver application submitted", driverApplicationService.submit(request)));
    }

    @GetMapping("/applications")
    public ResponseEntity<ApiResponse<List<DriverApplicationResponse>>> listApplications(
            @RequestParam(required = false) String status) {
        if (!authContext.isAdmin()) {
            throw new AccessDeniedException("Only admin can view driver applications");
        }
        return ResponseEntity.ok(
                ApiResponse.ok("Driver applications fetched", driverApplicationService.list(status)));
    }

    @PatchMapping("/applications/{applicationId}/status")
    public ResponseEntity<ApiResponse<DriverApplicationResponse>> updateApplicationStatus(
            @PathVariable Long applicationId, @Valid @RequestBody DriverApplicationStatusUpdateRequest request) {
        if (!authContext.isAdmin()) {
            throw new AccessDeniedException("Only admin can review driver applications");
        }
        DriverApplicationResponse response =
                driverApplicationService.updateStatus(applicationId, request.status(), "ADMIN");
        return ResponseEntity.ok(ApiResponse.ok("Driver application status updated", response));
    }

    @PostMapping("/{driverId}/ride-applications")
    public ResponseEntity<ApiResponse<RideApplicationResponse>> submitRideApplication(
            @PathVariable Long driverId, @Valid @RequestBody RideApplicationRequest request) {
        enforceDriverOwnership(driverId);
        if (!driverId.equals(request.driverId())) {
            throw new IllegalArgumentException("Driver mismatch");
        }
        return ResponseEntity.ok(
                ApiResponse.ok("Ride application submitted", rideApplicationService.submit(request)));
    }

    @GetMapping("/ride-applications")
    public ResponseEntity<ApiResponse<List<RideApplicationResponse>>> listRideApplications(
            @RequestParam(required = false) String status) {
        if (!authContext.isAdmin()) {
            throw new AccessDeniedException("Only admin can view ride applications");
        }
        return ResponseEntity.ok(
                ApiResponse.ok("Ride applications fetched", rideApplicationService.list(status)));
    }

    @PatchMapping("/ride-applications/{applicationId}/status")
    public ResponseEntity<ApiResponse<RideApplicationResponse>> updateRideApplicationStatus(
            @PathVariable Long applicationId, @Valid @RequestBody RideApplicationStatusUpdateRequest request) {
        if (!authContext.isAdmin()) {
            throw new AccessDeniedException("Only admin can review ride applications");
        }
        RideApplicationResponse response = rideApplicationService.updateStatus(applicationId, request.status());
        return ResponseEntity.ok(ApiResponse.ok("Ride application status updated", response));
    }

    @GetMapping("/{driverId}/rides")
    public ResponseEntity<ApiResponse<List<DriverRideResponse>>> getRides(@PathVariable Long driverId) {
        enforceDriverOwnership(driverId);
        return ResponseEntity.ok(ApiResponse.ok("Driver rides fetched", driverService.getDriverRides(driverId)));
    }

    @PatchMapping("/{driverId}/rides/{rideId}/status")
    public ResponseEntity<ApiResponse<DriverRideResponse>> updateRideStatus(
            @PathVariable Long driverId, @PathVariable Long rideId, @RequestParam RideStatus status) {
        enforceDriverOwnership(driverId);
        return ResponseEntity.ok(ApiResponse.ok(
                "Ride status updated", driverService.updateRideStatus(driverId, rideId, status)));
    }

    @PostMapping("/tracking")
    public ResponseEntity<ApiResponse<GpsPointResponse>> updateTracking(@Valid @RequestBody GpsUpdateRequest request) {
        enforceDriverOwnership(request.driverId());
        return ResponseEntity.ok(ApiResponse.ok("Tracking point recorded", driverService.addGpsPoint(request)));
    }

    @GetMapping("/{driverId}/tracking")
    public ResponseEntity<ApiResponse<List<GpsPointResponse>>> getTracking(@PathVariable Long driverId) {
        enforceDriverOwnership(driverId);
        return ResponseEntity.ok(ApiResponse.ok("Tracking points fetched", driverService.getDriverTracking(driverId)));
    }

    @GetMapping("/{driverId}/courier-orders")
    public ResponseEntity<ApiResponse<List<CourierOrderResponse>>> getCourierOrders(@PathVariable Long driverId) {
        enforceDriverOwnership(driverId);
        return ResponseEntity.ok(
                ApiResponse.ok("Courier orders fetched", courierOrderService.listForDriver(driverId)));
    }

    @PatchMapping("/{driverId}/courier-orders/{orderId}/status")
    public ResponseEntity<ApiResponse<CourierOrderResponse>> updateCourierStatus(
            @PathVariable Long driverId,
            @PathVariable Long orderId,
            @Valid @RequestBody CourierStatusPatchRequest request) {
        enforceDriverOwnership(driverId);
        return ResponseEntity.ok(ApiResponse.ok(
                "Courier status updated",
                courierOrderService.driverUpdateStatus(driverId, orderId, request.status())));
    }

    private void enforceDriverOwnership(Long targetDriverId) {
        if (authContext.isAdmin()) {
            return;
        }
        if (!targetDriverId.equals(authContext.currentDriverId())) {
            auditLogService.logDeniedAccess(
                    authContext.currentActorIdOrNull(),
                    "DRIVER",
                    targetDriverId,
                    "Driver ownership mismatch for driverId=" + targetDriverId);
            throw new AccessDeniedException("You are not allowed to access this driver resource");
        }
    }
}
