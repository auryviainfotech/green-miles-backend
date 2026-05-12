package com.greenmiles.backend.booking;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    boolean existsByRideRideIdAndSeatNumberAndBookingStatusIn(
            Long rideId, Integer seatNumber, List<BookingStatus> activeStatuses);

    List<Booking> findByUserUserIdOrderByBookingTimeDesc(Long userId);

    long countByBookingStatus(BookingStatus bookingStatus);
}
