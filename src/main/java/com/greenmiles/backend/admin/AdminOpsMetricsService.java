package com.greenmiles.backend.admin;

import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Component;

@Component
public class AdminOpsMetricsService {
    private final AtomicLong driverCreateSuccess = new AtomicLong(0);
    private final AtomicLong driverCreateFailure = new AtomicLong(0);
    private final AtomicLong routeCreateSuccess = new AtomicLong(0);
    private final AtomicLong routeCreateFailure = new AtomicLong(0);
    private final AtomicLong rideAssignSuccess = new AtomicLong(0);
    private final AtomicLong rideAssignFailure = new AtomicLong(0);

    public void incrementDriverCreateSuccess() {
        driverCreateSuccess.incrementAndGet();
    }

    public void incrementDriverCreateFailure() {
        driverCreateFailure.incrementAndGet();
    }

    public void incrementRouteCreateSuccess() {
        routeCreateSuccess.incrementAndGet();
    }

    public void incrementRouteCreateFailure() {
        routeCreateFailure.incrementAndGet();
    }

    public void incrementRideAssignSuccess() {
        rideAssignSuccess.incrementAndGet();
    }

    public void incrementRideAssignFailure() {
        rideAssignFailure.incrementAndGet();
    }

    public AdminOpsMetricsSnapshot snapshot() {
        return new AdminOpsMetricsSnapshot(
                driverCreateSuccess.get(),
                driverCreateFailure.get(),
                routeCreateSuccess.get(),
                routeCreateFailure.get(),
                rideAssignSuccess.get(),
                rideAssignFailure.get());
    }

    public record AdminOpsMetricsSnapshot(
            long driverCreateSuccess,
            long driverCreateFailure,
            long routeCreateSuccess,
            long routeCreateFailure,
            long rideAssignSuccess,
            long rideAssignFailure) {}
}

