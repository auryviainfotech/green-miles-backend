package com.greenmiles.backend.courier;

import com.greenmiles.backend.courier.dto.CourierCreateOrderRequest;
import com.greenmiles.backend.courier.dto.CourierOrderResponse;
import com.greenmiles.backend.driver.Driver;
import com.greenmiles.backend.driver.DriverRepository;
import com.greenmiles.backend.driver.GpsTracking;
import com.greenmiles.backend.driver.GpsTrackingRepository;
import com.greenmiles.backend.driver.dto.GpsPointResponse;
import com.greenmiles.backend.notification.NotificationService;
import com.greenmiles.backend.notification.NotificationType;
import com.greenmiles.backend.notification.dto.CreateNotificationRequest;
import com.greenmiles.backend.ride.CarRepository;
import com.greenmiles.backend.user.User;
import com.greenmiles.backend.user.UserRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CourierOrderService {

    public static final String ENTITY = "COURIER_ORDER";
    public static final String STATUS_PICKUP_PENDING = "pickup_pending";
    public static final String STATUS_PARTNER_ASSIGNED = "partner_assigned";
    public static final String STATUS_EN_ROUTE_PICKUP = "en_route_pickup";
    public static final String STATUS_PICKED_UP = "picked_up";
    public static final String STATUS_IN_TRANSIT = "in_transit";
    public static final String STATUS_OUT_FOR_DELIVERY = "out_for_delivery";
    public static final String STATUS_DELIVERED = "delivered";
    public static final String STATUS_CANCELLED = "cancelled";

    private static final Set<String> ADMIN_STATUSES = Set.of(
            STATUS_PICKUP_PENDING,
            STATUS_PARTNER_ASSIGNED,
            STATUS_EN_ROUTE_PICKUP,
            STATUS_PICKED_UP,
            STATUS_IN_TRANSIT,
            STATUS_OUT_FOR_DELIVERY,
            STATUS_DELIVERED,
            STATUS_CANCELLED);

    private final CourierOrderRepository courierOrderRepository;
    private final UserRepository userRepository;
    private final DriverRepository driverRepository;
    private final CarRepository carRepository;
    private final NotificationService notificationService;
    private final GpsTrackingRepository gpsTrackingRepository;

    public CourierOrderService(
            CourierOrderRepository courierOrderRepository,
            UserRepository userRepository,
            DriverRepository driverRepository,
            CarRepository carRepository,
            NotificationService notificationService,
            GpsTrackingRepository gpsTrackingRepository) {
        this.courierOrderRepository = courierOrderRepository;
        this.userRepository = userRepository;
        this.driverRepository = driverRepository;
        this.carRepository = carRepository;
        this.notificationService = notificationService;
        this.gpsTrackingRepository = gpsTrackingRepository;
    }

    @Transactional
    public CourierOrderResponse create(CourierCreateOrderRequest request) {
        User user = userRepository.findById(request.userId()).orElseThrow(() -> new IllegalArgumentException("User not found"));
        String packageCategory = request.packageCategory() == null || request.packageCategory().isBlank()
                ? "PARCEL"
                : request.packageCategory().trim().toUpperCase();
        Instant now = Instant.now();
        int etaMins = Math.max(5, (int) Math.round(request.distanceKm() * 2.5));

        CourierOrder order = new CourierOrder();
        order.setUser(user);
        order.setAwbNumber(generateAwbNumber());
        order.setQrToken(UUID.randomUUID().toString().replace("-", ""));
        order.setPickupAddress(request.pickup().trim());
        order.setDropAddress(request.drop().trim());
        order.setWeightKg(java.math.BigDecimal.valueOf(request.weight()));
        order.setDistanceKm(java.math.BigDecimal.valueOf(request.distanceKm()));
        order.setPackageCategory(packageCategory);
        order.setRecipientName(toNullIfBlank(request.recipientName()));
        order.setRecipientPhone(toNullIfBlank(request.recipientPhone()));
        order.setContentsNote(toNullIfBlank(request.contentsNote()));
        order.setPickupSlotLabel(toNullIfBlank(request.pickupSlotLabel()));
        order.setContactPhone(request.contactPhone());
        order.setContactEmail(request.contactEmail().trim());
        order.setPriceAmount(request.price());
        order.setEtaMins(etaMins);
        order.setStatus(STATUS_PICKUP_PENDING);
        order.setCreatedAt(now);
        order.setUpdatedAt(now);

        CourierOrder saved = courierOrderRepository.save(order);
        notificationService.create(new CreateNotificationRequest(
                saved.getUser().getUserId(),
                "Courier booking confirmed",
                "AWB " + saved.getAwbNumber() + " created. We are assigning a delivery partner.",
                NotificationType.COURIER_UPDATE));
        return toResponse(saved, lastGpsPoint(saved.getCourierOrderId()));
    }

    @Transactional(readOnly = true)
    public CourierOrderResponse getById(Long orderId) {
        CourierOrder order = courierOrderRepository
                .findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Courier order not found"));
        return toResponse(order, lastGpsPoint(orderId));
    }

    @Transactional(readOnly = true)
    public List<CourierOrderResponse> listForUser(Long userId) {
        List<CourierOrderResponse> out = new ArrayList<>();
        for (CourierOrder order : courierOrderRepository.findByUser_UserIdOrderByCreatedAtDesc(userId)) {
            out.add(toResponse(order, lastGpsPoint(order.getCourierOrderId())));
        }
        return out;
    }

    @Transactional(readOnly = true)
    public List<CourierOrderResponse> listAll() {
        List<CourierOrderResponse> out = new ArrayList<>();
        for (CourierOrder order : courierOrderRepository.findAllByOrderByCreatedAtDesc()) {
            out.add(toResponse(order, lastGpsPoint(order.getCourierOrderId())));
        }
        return out;
    }

    @Transactional(readOnly = true)
    public List<CourierOrderResponse> listForDriver(Long driverId) {
        if (!driverRepository.existsById(driverId)) {
            throw new IllegalArgumentException("Driver not found");
        }
        List<CourierOrderResponse> out = new ArrayList<>();
        for (CourierOrder order : courierOrderRepository.findByDriver_DriverIdOrderByCreatedAtDesc(driverId)) {
            out.add(toResponse(order, lastGpsPoint(order.getCourierOrderId())));
        }
        return out;
    }

    @Transactional(readOnly = true)
    public List<GpsPointResponse> getTracking(Long orderId) {
        List<GpsTracking> points = gpsTrackingRepository.findTop50ByCourierOrder_CourierOrderIdOrderByRecordedAtDesc(orderId);
        List<GpsPointResponse> out = new ArrayList<>();
        for (int i = points.size() - 1; i >= 0; i--) {
            GpsTracking p = points.get(i);
            out.add(new GpsPointResponse(
                    p.getGpsId(),
                    p.getDriver().getDriverId(),
                    p.getRide() != null ? p.getRide().getRideId() : null,
                    p.getCourierOrder() != null ? p.getCourierOrder().getCourierOrderId() : null,
                    p.getLatitude(),
                    p.getLongitude(),
                    p.getSpeed(),
                    p.getHeading(),
                    p.getRecordedAt()));
        }
        return out;
    }

    @Transactional
    public CourierOrderResponse assign(Long orderId, Long driverId) {
        CourierOrder order = courierOrderRepository
                .findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Courier order not found"));
        if (!STATUS_PICKUP_PENDING.equals(order.getStatus())) {
            throw new IllegalArgumentException("Can only assign while pickup is pending");
        }
        Driver driver = driverRepository.findById(driverId).orElseThrow(() -> new IllegalArgumentException("Driver not found"));
        order.setDriver(driver);
        order.setAssignedAt(Instant.now());
        order.setStatus(STATUS_PARTNER_ASSIGNED);
        order.setUpdatedAt(Instant.now());
        CourierOrder saved = courierOrderRepository.save(order);
        notificationService.create(new CreateNotificationRequest(
                saved.getUser().getUserId(),
                "Delivery partner assigned",
                "AWB " + saved.getAwbNumber() + " assigned to " + driver.getFullName() + ".",
                NotificationType.COURIER_UPDATE));
        return toResponse(saved, lastGpsPoint(saved.getCourierOrderId()));
    }

    @Transactional
    public CourierOrderResponse cancelByUser(Long orderId, Long userId, String reason) {
        CourierOrder order = courierOrderRepository
                .findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Courier order not found"));
        if (!order.getUser().getUserId().equals(userId)) {
            throw new IllegalArgumentException("Courier order not found");
        }
        if (!Set.of(STATUS_PICKUP_PENDING, STATUS_PARTNER_ASSIGNED).contains(order.getStatus())) {
            throw new IllegalArgumentException("This shipment can no longer be cancelled in-app");
        }
        order.setStatus(STATUS_CANCELLED);
        order.setCancelReason(reason.trim());
        order.setUpdatedAt(Instant.now());
        CourierOrder saved = courierOrderRepository.save(order);
        notificationService.create(new CreateNotificationRequest(
                saved.getUser().getUserId(),
                "Courier order cancelled",
                "AWB " + saved.getAwbNumber() + " was cancelled successfully.",
                NotificationType.COURIER_UPDATE));
        return toResponse(saved, lastGpsPoint(saved.getCourierOrderId()));
    }

    @Transactional
    public CourierOrderResponse adminUpdateStatus(Long orderId, String status) {
        String normalized = status.trim().toLowerCase();
        if (!ADMIN_STATUSES.contains(normalized)) {
            throw new IllegalArgumentException("Invalid courier status");
        }
        CourierOrder order = courierOrderRepository
                .findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Courier order not found"));
        order.setStatus(normalized);
        if (STATUS_DELIVERED.equals(normalized)) {
            order.setEtaMins(0);
        }
        order.setUpdatedAt(Instant.now());
        CourierOrder saved = courierOrderRepository.save(order);
        if (STATUS_DELIVERED.equals(normalized)) {
            notificationService.create(new CreateNotificationRequest(
                    saved.getUser().getUserId(),
                    "Parcel delivered",
                    "AWB " + saved.getAwbNumber() + " marked as delivered.",
                    NotificationType.COURIER_UPDATE));
        }
        return toResponse(saved, lastGpsPoint(saved.getCourierOrderId()));
    }

    @Transactional
    public CourierOrderResponse driverUpdateStatus(Long driverId, Long orderId, String status) {
        String normalized = status.trim().toLowerCase();
        CourierOrder order = courierOrderRepository
                .findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Courier order not found"));
        if (order.getDriver() == null || !order.getDriver().getDriverId().equals(driverId)) {
            throw new IllegalArgumentException("Courier order is not assigned to this driver");
        }
        enforceDriverTransition(order.getStatus(), normalized);
        order.setStatus(normalized);
        if (STATUS_DELIVERED.equals(normalized)) {
            order.setEtaMins(0);
        }
        order.setUpdatedAt(Instant.now());
        CourierOrder saved = courierOrderRepository.save(order);
        if (STATUS_DELIVERED.equals(normalized)) {
            notificationService.create(new CreateNotificationRequest(
                    saved.getUser().getUserId(),
                    "Parcel delivered",
                    "AWB " + saved.getAwbNumber() + " marked as delivered.",
                    NotificationType.COURIER_UPDATE));
        }
        return toResponse(saved, lastGpsPoint(saved.getCourierOrderId()));
    }

    private void enforceDriverTransition(String current, String next) {
        switch (current) {
            case STATUS_PARTNER_ASSIGNED -> {
                if (!Set.of(STATUS_EN_ROUTE_PICKUP, STATUS_PICKED_UP).contains(next)) {
                    throw new IllegalArgumentException("Invalid next status from partner_assigned");
                }
            }
            case STATUS_EN_ROUTE_PICKUP -> {
                if (!STATUS_PICKED_UP.equals(next)) {
                    throw new IllegalArgumentException("Invalid next status from en_route_pickup");
                }
            }
            case STATUS_PICKED_UP -> {
                if (!STATUS_IN_TRANSIT.equals(next)) {
                    throw new IllegalArgumentException("Invalid next status from picked_up");
                }
            }
            case STATUS_IN_TRANSIT -> {
                if (!STATUS_OUT_FOR_DELIVERY.equals(next)) {
                    throw new IllegalArgumentException("Invalid next status from in_transit");
                }
            }
            case STATUS_OUT_FOR_DELIVERY -> {
                if (!STATUS_DELIVERED.equals(next)) {
                    throw new IllegalArgumentException("Invalid next status from out_for_delivery");
                }
            }
            default -> throw new IllegalArgumentException("Driver cannot update from status " + current);
        }
    }

    private String toNullIfBlank(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String generateAwbNumber() {
        for (int i = 0; i < 24; i++) {
            String candidate = "GM" + Long.toUnsignedString(System.nanoTime(), 36).toUpperCase()
                    + Integer.toString(ThreadLocalRandom.current().nextInt(1296, 46656), 36).toUpperCase();
            if (candidate.length() > 32) {
                candidate = candidate.substring(0, 32);
            }
            if (!courierOrderRepository.existsByAwbNumber(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("Failed to allocate AWB");
    }

    private Optional<GpsTracking> lastGpsPoint(Long orderId) {
        return gpsTrackingRepository.findFirstByCourierOrder_CourierOrderIdOrderByRecordedAtDesc(orderId);
    }

    private CourierOrderResponse toResponse(CourierOrder order, Optional<GpsTracking> lastGpsOpt) {
        GpsTracking lastGps = lastGpsOpt.orElse(null);
        Long driverId = order.getDriver() != null ? order.getDriver().getDriverId() : null;
        String driverName = order.getDriver() != null ? order.getDriver().getFullName() : null;
        String vehicleHint = driverId == null
                ? null
                : carRepository.findFirstByDriverId(driverId).map(c -> "Car #" + c.getCarId()).orElse(null);

        return new CourierOrderResponse(
                order.getCourierOrderId(),
                order.getAwbNumber(),
                order.getQrToken(),
                order.getUser().getUserId(),
                driverId,
                driverName,
                vehicleHint,
                order.getPickupAddress(),
                order.getDropAddress(),
                order.getWeightKg().doubleValue(),
                order.getDistanceKm().doubleValue(),
                order.getPackageCategory(),
                order.getRecipientName(),
                order.getRecipientPhone(),
                order.getContentsNote(),
                order.getPickupSlotLabel(),
                order.getPriceAmount(),
                order.getStatus(),
                order.getEtaMins(),
                order.getContactPhone(),
                order.getContactEmail(),
                order.getCreatedAt(),
                order.getCancelReason(),
                lastGps != null ? lastGps.getLatitude().doubleValue() : null,
                lastGps != null ? lastGps.getLongitude().doubleValue() : null,
                lastGps != null ? lastGps.getRecordedAt() : null);
    }
}
