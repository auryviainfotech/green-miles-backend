package com.greenmiles.backend.admin;

import com.greenmiles.backend.admin.dto.AdminAuditLogPageResponse;
import com.greenmiles.backend.admin.dto.AdminAuditLogResponse;
import com.greenmiles.backend.admin.dto.AdminCreateDriverRequest;
import com.greenmiles.backend.admin.dto.AdminDashboardResponse;
import com.greenmiles.backend.admin.dto.AdminDriverResponse;
import com.greenmiles.backend.admin.dto.AdminOpsMetricsResponse;
import com.greenmiles.backend.admin.dto.PaymentLogResponse;
import com.greenmiles.backend.admin.dto.RideAssignmentRequest;
import com.greenmiles.backend.admin.dto.RideAssignmentResponse;
import com.greenmiles.backend.admin.dto.RoutePlanCreateRequest;
import com.greenmiles.backend.admin.dto.RoutePlanResponse;
import com.greenmiles.backend.audit.AuditLog;
import com.greenmiles.backend.audit.AuditLogRepository;
import com.greenmiles.backend.booking.BookingRepository;
import com.greenmiles.backend.booking.BookingStatus;
import com.greenmiles.backend.driver.Driver;
import com.greenmiles.backend.driver.DriverRepository;
import com.greenmiles.backend.driver.DriverStatus;
import com.greenmiles.backend.notification.NotificationRepository;
import com.greenmiles.backend.ride.Car;
import com.greenmiles.backend.ride.CarRepository;
import com.greenmiles.backend.ride.City;
import com.greenmiles.backend.ride.CityRepository;
import com.greenmiles.backend.ride.Ride;
import com.greenmiles.backend.ride.RideEntityRepository;
import com.greenmiles.backend.ride.RideStatus;
import com.greenmiles.backend.user.UserRepository;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminService {

    private final UserRepository userRepository;
    private final DriverRepository driverRepository;
    private final RideEntityRepository rideRepository;
    private final BookingRepository bookingRepository;
    private final NotificationRepository notificationRepository;
    private final AuditLogRepository auditLogRepository;
    private final RoutePlanRepository routePlanRepository;
    private final PaymentLogRepository paymentLogRepository;
    private final CityRepository cityRepository;
    private final CarRepository carRepository;
    private final DriverProfileRepository driverProfileRepository;
    private final AdminOpsMetricsService adminOpsMetricsService;

    public AdminService(
            UserRepository userRepository,
            DriverRepository driverRepository,
            RideEntityRepository rideRepository,
            BookingRepository bookingRepository,
            NotificationRepository notificationRepository,
            AuditLogRepository auditLogRepository,
            RoutePlanRepository routePlanRepository,
            PaymentLogRepository paymentLogRepository,
            CityRepository cityRepository,
            CarRepository carRepository,
            DriverProfileRepository driverProfileRepository,
            AdminOpsMetricsService adminOpsMetricsService) {
        this.userRepository = userRepository;
        this.driverRepository = driverRepository;
        this.rideRepository = rideRepository;
        this.bookingRepository = bookingRepository;
        this.notificationRepository = notificationRepository;
        this.auditLogRepository = auditLogRepository;
        this.routePlanRepository = routePlanRepository;
        this.paymentLogRepository = paymentLogRepository;
        this.cityRepository = cityRepository;
        this.carRepository = carRepository;
        this.driverProfileRepository = driverProfileRepository;
        this.adminOpsMetricsService = adminOpsMetricsService;
    }

    @Transactional(readOnly = true)
    public AdminDashboardResponse getDashboard() {
        long totalRides = rideRepository.count();
        long activeRides = rideRepository.countByRideStatus(RideStatus.ACTIVE);
        long totalBookings = bookingRepository.count();
        long pendingBookings = bookingRepository.countByBookingStatus(BookingStatus.PENDING);
        long unreadNotifications = notificationRepository.countByReadFalse();
        long totalRoutePlans = routePlanRepository.count();
        long totalPaymentLogs = paymentLogRepository.count();
        long successfulPayments = paymentLogRepository.countByStatus("SUCCESS");
        long failedPayments = paymentLogRepository.countByStatus("FAILED");
        long totalPaymentAmount = paymentLogRepository.sumAmountByStatus("SUCCESS");
        return new AdminDashboardResponse(
                userRepository.count(),
                driverRepository.count(),
                totalRides,
                activeRides,
                totalBookings,
                pendingBookings,
                unreadNotifications,
                totalRoutePlans,
                totalPaymentLogs,
                successfulPayments,
                failedPayments,
                totalPaymentAmount);
    }

    @Transactional(readOnly = true)
    public List<AdminDriverResponse> listDrivers() {
        List<Driver> drivers = driverRepository.findAll();
        final Map<Long, DriverProfile> profileByDriverId;
        List<Long> driverIds = drivers.stream().map(Driver::getDriverId).toList();
        if (driverIds.isEmpty()) {
            profileByDriverId = new HashMap<>();
        } else {
            profileByDriverId = driverProfileRepository.findByDriverDriverIdIn(driverIds).stream()
                    .collect(java.util.stream.Collectors.toMap(
                            p -> p.getDriver().getDriverId(),
                            p -> p,
                            (left, right) -> left));
        }
        return drivers.stream()
                .map(driver -> {
                    Car primaryCar = carRepository.findFirstByDriverId(driver.getDriverId()).orElse(null);
                    return toDriverResponse(driver, primaryCar, profileByDriverId.get(driver.getDriverId()));
                })
                .toList();
    }

    @Transactional
    public AdminDriverResponse createDriver(AdminCreateDriverRequest request) {
        try {
            if (driverRepository.existsByPhone(request.phone())) {
                throw new IllegalArgumentException("Driver phone already exists");
            }
            if (driverRepository.existsByLicenseNumber(request.licenseNumber())) {
                throw new IllegalArgumentException("Driver license already exists");
            }

            Driver driver = new Driver();
            driver.setFullName(request.fullName().trim());
            driver.setPhone(request.phone().trim());
            driver.setLicenseNumber(request.licenseNumber().trim().toUpperCase());
            driver.setStatus(DriverStatus.valueOf(request.status().trim().toUpperCase()));
            driver.setRating(request.rating());
            Driver savedDriver = driverRepository.save(driver);

            Car car = new Car();
            car.setDriverId(savedDriver.getDriverId());
            car.setVehicleNumber(request.vehicleNumber().trim().toUpperCase());
            car.setRcNumber(request.rcNumber().trim().toUpperCase());
            car.setVehicleType(request.vehicleType().trim().toUpperCase());
            car.setTotalSeats(request.totalSeats());
            car.setStatus(request.carStatus().trim().toUpperCase());
            Car savedCar = carRepository.save(car);

            DriverProfile profile = new DriverProfile();
            profile.setDriver(savedDriver);
            profile.setEmail(blankToNull(request.email()));
            profile.setAddress(blankToNull(request.address()));
            profile.setVidProofNumber(blankToNull(request.vidProofNumber()));
            DriverProfile savedProfile = driverProfileRepository.save(profile);

            adminOpsMetricsService.incrementDriverCreateSuccess();
            return toDriverResponse(savedDriver, savedCar, savedProfile);
        } catch (RuntimeException ex) {
            adminOpsMetricsService.incrementDriverCreateFailure();
            throw ex;
        }
    }

    @Transactional(readOnly = true)
    public List<RoutePlanResponse> listRoutes() {
        return routePlanRepository.findAll().stream().map(this::toRouteResponse).toList();
    }

    @Transactional
    public RoutePlanResponse createRoute(RoutePlanCreateRequest request) {
        try {
            if (request.startCityId().equals(request.endCityId())) {
                throw new IllegalArgumentException("Start and end city cannot be same");
            }
            City start = cityRepository
                    .findById(request.startCityId())
                    .orElseThrow(() -> new IllegalArgumentException("Start city not found"));
            City end = cityRepository
                    .findById(request.endCityId())
                    .orElseThrow(() -> new IllegalArgumentException("End city not found"));

            RoutePlan routePlan = new RoutePlan();
            routePlan.setRouteCode(resolveRouteCode(request, start, end));
            routePlan.setStartCity(start);
            routePlan.setEndCity(end);
            routePlan.setBaseFare(request.baseFare());
            routePlan.setDefaultSeats(request.defaultSeats());
            routePlan.setActive(Boolean.TRUE);
            routePlan.setCreatedAt(Instant.now());
            RoutePlan saved = routePlanRepository.save(routePlan);
            adminOpsMetricsService.incrementRouteCreateSuccess();
            return toRouteResponse(saved);
        } catch (RuntimeException ex) {
            adminOpsMetricsService.incrementRouteCreateFailure();
            throw ex;
        }
    }

    @Transactional(readOnly = true)
    public List<RideAssignmentResponse> listRideAssignments() {
        List<Ride> rides = rideRepository.findAllByOrderByStartTimeDesc();
        if (rides.isEmpty()) {
            return List.of();
        }

        Set<Long> carIds = rides.stream().map(Ride::getCarId).collect(java.util.stream.Collectors.toSet());
        Set<Long> driverIds = rides.stream().map(Ride::getDriverId).collect(java.util.stream.Collectors.toSet());
        Map<Long, Car> carById = carIds.isEmpty()
                ? Map.of()
                : carRepository.findAllById(carIds).stream()
                        .collect(java.util.stream.Collectors.toMap(Car::getCarId, c -> c));
        Map<Long, Driver> driverById = driverIds.isEmpty()
                ? Map.of()
                : driverRepository.findAllById(driverIds).stream()
                        .collect(java.util.stream.Collectors.toMap(Driver::getDriverId, d -> d));

        return rides.stream()
                .map(ride -> {
                    Car car = carById.get(ride.getCarId());
                    String vehicleNumber = car == null ? null : car.getVehicleNumber();
                    return new RideAssignmentResponse(
                            ride.getRideId(),
                            null,
                            "RIDE-" + ride.getRideId(),
                            ride.getDriverId(),
                            driverById.containsKey(ride.getDriverId())
                                    ? driverById.get(ride.getDriverId()).getFullName()
                                    : "Driver #" + ride.getDriverId(),
                            ride.getCarId(),
                            vehicleNumber,
                            ride.getStartCity().getCityName(),
                            ride.getEndCity().getCityName(),
                            ride.getStartTime(),
                            ride.getRideStatus().name(),
                            ride.getAvailableSeats());
                })
                .toList();
    }

    @Transactional
    public RideAssignmentResponse assignRide(RideAssignmentRequest request) {
        try {
            RoutePlan route = routePlanRepository
                    .findById(request.routePlanId())
                    .orElseThrow(() -> new IllegalArgumentException("Route plan not found"));
            Driver driver = driverRepository
                    .findById(request.driverId())
                    .orElseThrow(() -> new IllegalArgumentException("Driver not found"));
            Car car = carRepository
                    .findByCarIdAndDriverId(request.carId(), request.driverId())
                    .orElseThrow(() -> new IllegalArgumentException("Car is not mapped to selected driver"));

            Ride ride = new Ride();
            ride.setDriverId(driver.getDriverId());
            ride.setCarId(car.getCarId());
            ride.setStartCity(route.getStartCity());
            ride.setEndCity(route.getEndCity());
            ride.setStartTime(LocalDateTime.parse(request.startTime().trim()));
            ride.setRideStatus(RideStatus.ACTIVE);
            ride.setAvailableSeats(request.availableSeats());
            Ride savedRide = rideRepository.save(ride);
            adminOpsMetricsService.incrementRideAssignSuccess();
            return new RideAssignmentResponse(
                    savedRide.getRideId(),
                    route.getRoutePlanId(),
                    route.getRouteCode(),
                    driver.getDriverId(),
                    driver.getFullName(),
                    car.getCarId(),
                    car.getVehicleNumber(),
                    route.getStartCity().getCityName(),
                    route.getEndCity().getCityName(),
                    savedRide.getStartTime(),
                    savedRide.getRideStatus().name(),
                    savedRide.getAvailableSeats());
        } catch (RuntimeException ex) {
            adminOpsMetricsService.incrementRideAssignFailure();
            throw ex;
        }
    }

    @Transactional(readOnly = true)
    public List<PaymentLogResponse> listPaymentLogs() {
        return paymentLogRepository.findAll().stream()
                .map(log -> new PaymentLogResponse(
                        log.getPaymentLogId(),
                        log.getBooking().getBookingId(),
                        log.getUser().getUserId(),
                        log.getRide().getRideId(),
                        log.getAmount(),
                        log.getStatus(),
                        log.getMethod(),
                        log.getReferenceCode(),
                        log.getCreatedAt()))
                .toList();
    }

    @Transactional(readOnly = true)
    public AdminAuditLogPageResponse getDeniedAuditLogs(int page, int size, String entity) {
        PageRequest pageRequest = PageRequest.of(page, size);
        Page<AuditLog> resultPage;
        if (entity == null || entity.isBlank()) {
            resultPage = auditLogRepository.findByActionOrderByCreatedAtDesc("ACCESS_DENIED", pageRequest);
        } else {
            resultPage = auditLogRepository.findByActionAndEntityOrderByCreatedAtDesc(
                    "ACCESS_DENIED", entity.trim().toUpperCase(), pageRequest);
        }
        return new AdminAuditLogPageResponse(
                resultPage.getContent().stream().map(this::toAuditResponse).toList(),
                resultPage.getNumber(),
                resultPage.getSize(),
                resultPage.getTotalElements(),
                resultPage.getTotalPages());
    }

    @Transactional(readOnly = true)
    public AdminOpsMetricsResponse getOpsMetrics() {
        var snap = adminOpsMetricsService.snapshot();
        return new AdminOpsMetricsResponse(
                snap.driverCreateSuccess(),
                snap.driverCreateFailure(),
                snap.routeCreateSuccess(),
                snap.routeCreateFailure(),
                snap.rideAssignSuccess(),
                snap.rideAssignFailure());
    }

    private AdminAuditLogResponse toAuditResponse(AuditLog log) {
        return new AdminAuditLogResponse(
                log.getLogId(),
                log.getUserId(),
                log.getAction(),
                log.getEntity(),
                log.getEntityId(),
                log.getDetails(),
                log.getCreatedAt());
    }

    private AdminDriverResponse toDriverResponse(Driver driver, Car car, DriverProfile profile) {
        return new AdminDriverResponse(
                driver.getDriverId(),
                driver.getFullName(),
                driver.getPhone(),
                driver.getLicenseNumber(),
                profile == null ? null : profile.getEmail(),
                profile == null ? null : profile.getAddress(),
                profile == null ? null : profile.getVidProofNumber(),
                driver.getStatus().name(),
                driver.getRating(),
                car == null ? null : car.getCarId(),
                car == null ? null : car.getVehicleNumber(),
                car == null ? null : car.getRcNumber(),
                car == null ? null : car.getVehicleType(),
                car == null ? null : car.getTotalSeats(),
                car == null ? null : car.getStatus());
    }

    private RoutePlanResponse toRouteResponse(RoutePlan route) {
        return new RoutePlanResponse(
                route.getRoutePlanId(),
                route.getRouteCode(),
                route.getStartCity().getCityId(),
                route.getStartCity().getCityName(),
                route.getEndCity().getCityId(),
                route.getEndCity().getCityName(),
                route.getBaseFare(),
                route.getDefaultSeats(),
                route.getActive(),
                route.getCreatedAt());
    }

    private String resolveRouteCode(RoutePlanCreateRequest request, City start, City end) {
        return (start.getCityName().substring(0, Math.min(3, start.getCityName().length()))
                        + "-"
                        + end.getCityName().substring(0, Math.min(3, end.getCityName().length()))
                        + "-"
                        + System.currentTimeMillis())
                .toUpperCase();
    }

    private String blankToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
