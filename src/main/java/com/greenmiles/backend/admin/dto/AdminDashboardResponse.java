package com.greenmiles.backend.admin.dto;

public record AdminDashboardResponse(
        long totalUsers,
        long totalDrivers,
        long totalRides,
        long activeRides,
        long totalBookings,
        long pendingBookings,
        long unreadNotifications,
        long totalRoutePlans,
        long totalPaymentLogs,
        long successfulPayments,
        long failedPayments,
        long totalPaymentAmount) {}
