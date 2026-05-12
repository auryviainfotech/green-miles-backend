package com.greenmiles.backend.driver;

import com.greenmiles.backend.courier.CourierOrder;
import com.greenmiles.backend.courier.CourierOrderRepository;
import com.greenmiles.backend.courier.CourierOrderService;
import com.greenmiles.backend.driver.dto.DriverRideResponse;
import com.greenmiles.backend.driver.dto.GpsPointResponse;
import com.greenmiles.backend.driver.dto.GpsUpdateRequest;
import com.greenmiles.backend.ride.Ride;
import com.greenmiles.backend.ride.RideEntityRepository;
import com.greenmiles.backend.ride.RideStatus;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DriverService {

    private final DriverRepository driverRepository;
    private final RideEntityRepository rideRepository;
    private final GpsTrackingRepository gpsTrackingRepository;
    private final CourierOrderRepository courierOrderRepository;

    public DriverService(
            DriverRepository driverRepository,
            RideEntityRepository rideRepository,
            GpsTrackingRepository gpsTrackingRepository,
            CourierOrderRepository courierOrderRepository) {
        this.driverRepository = driverRepository;
        this.rideRepository = rideRepository;
        this.gpsTrackingRepository = gpsTrackingRepository;
        this.courierOrderRepository = courierOrderRepository;
    }

    @Transactional(readOnly = true)
    public List<DriverRideResponse> getDriverRides(Long driverId) {
        if (!driverRepository.existsById(driverId)) {
            throw new IllegalArgumentException("Driver not found");
        }
        return rideRepository.findByDriverIdOrderByStartTimeDesc(driverId).stream()
                .map(r -> new DriverRideResponse(
                        r.getRideId(), r.getDriverId(), r.getRideStatus(), r.getStartTime(), r.getAvailableSeats()))
                .toList();
    }

    @Transactional
    public DriverRideResponse updateRideStatus(Long driverId, Long rideId, RideStatus rideStatus) {
        if (!driverRepository.existsById(driverId)) {
            throw new IllegalArgumentException("Driver not found");
        }
        Ride ride = rideRepository.findById(rideId).orElseThrow(() -> new IllegalArgumentException("Ride not found"));
        if (!ride.getDriverId().equals(driverId)) {
            throw new IllegalArgumentException("Ride does not belong to driver");
        }
        ride.setRideStatus(rideStatus);
        Ride saved = rideRepository.save(ride);
        return new DriverRideResponse(
                saved.getRideId(), saved.getDriverId(), saved.getRideStatus(), saved.getStartTime(), saved.getAvailableSeats());
    }

    @Transactional
    public GpsPointResponse addGpsPoint(GpsUpdateRequest request) {
        Driver driver =
                driverRepository.findById(request.driverId()).orElseThrow(() -> new IllegalArgumentException("Driver not found"));
        Ride ride = null;
        CourierOrder courierOrder = null;
        if (request.courierOrderId() != null) {
            courierOrder = courierOrderRepository
                    .findById(request.courierOrderId())
                    .orElseThrow(() -> new IllegalArgumentException("Courier order not found"));
            if (courierOrder.getDriver() == null || !courierOrder.getDriver().getDriverId().equals(request.driverId())) {
                throw new IllegalArgumentException("Courier order is not assigned to this driver");
            }
            if (CourierOrderService.STATUS_PICKUP_PENDING.equals(courierOrder.getStatus())
                    || CourierOrderService.STATUS_CANCELLED.equals(courierOrder.getStatus())
                    || CourierOrderService.STATUS_DELIVERED.equals(courierOrder.getStatus())) {
                throw new IllegalArgumentException("Courier GPS updates are not allowed in current status");
            }
        } else if (request.rideId() != null) {
            ride = rideRepository.findById(request.rideId()).orElseThrow(() -> new IllegalArgumentException("Ride not found"));
        }

        GpsTracking gps = new GpsTracking();
        gps.setDriver(driver);
        gps.setRide(ride);
        gps.setCourierOrder(courierOrder);
        gps.setLatitude(request.latitude());
        gps.setLongitude(request.longitude());
        gps.setSpeed(request.speed());
        gps.setHeading(request.heading());
        gps.setRecordedAt(LocalDateTime.now());
        gps.setCreatedAt(Instant.now());
        return toPoint(gpsTrackingRepository.save(gps));
    }

    @Transactional(readOnly = true)
    public List<GpsPointResponse> getDriverTracking(Long driverId) {
        return gpsTrackingRepository.findTop20ByDriverDriverIdOrderByRecordedAtDesc(driverId).stream()
                .map(this::toPoint)
                .toList();
    }

    private GpsPointResponse toPoint(GpsTracking gps) {
        return new GpsPointResponse(
                gps.getGpsId(),
                gps.getDriver().getDriverId(),
                gps.getRide() != null ? gps.getRide().getRideId() : null,
                gps.getCourierOrder() != null ? gps.getCourierOrder().getCourierOrderId() : null,
                gps.getLatitude(),
                gps.getLongitude(),
                gps.getSpeed(),
                gps.getHeading(),
                gps.getRecordedAt());
    }
}
