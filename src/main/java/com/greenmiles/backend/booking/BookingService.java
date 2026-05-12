package com.greenmiles.backend.booking;

import com.greenmiles.backend.admin.PaymentLog;
import com.greenmiles.backend.admin.PaymentLogRepository;
import com.greenmiles.backend.booking.dto.BookingResponse;
import com.greenmiles.backend.booking.dto.CreateBookingRequest;
import com.greenmiles.backend.notification.Notification;
import com.greenmiles.backend.notification.NotificationRepository;
import com.greenmiles.backend.notification.NotificationType;
import com.greenmiles.backend.ride.BoardingPoint;
import com.greenmiles.backend.ride.BoardingPointRepository;
import com.greenmiles.backend.ride.Ride;
import com.greenmiles.backend.ride.RideEntityRepository;
import com.greenmiles.backend.ride.RideStatus;
import com.greenmiles.backend.user.User;
import com.greenmiles.backend.user.UserRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final CancellationRepository cancellationRepository;
    private final UserRepository userRepository;
    private final RideEntityRepository rideRepository;
    private final BoardingPointRepository boardingPointRepository;
    private final NotificationRepository notificationRepository;
    private final PaymentLogRepository paymentLogRepository;

    public BookingService(
            BookingRepository bookingRepository,
            CancellationRepository cancellationRepository,
            UserRepository userRepository,
            RideEntityRepository rideRepository,
            BoardingPointRepository boardingPointRepository,
            NotificationRepository notificationRepository,
            PaymentLogRepository paymentLogRepository) {
        this.bookingRepository = bookingRepository;
        this.cancellationRepository = cancellationRepository;
        this.userRepository = userRepository;
        this.rideRepository = rideRepository;
        this.boardingPointRepository = boardingPointRepository;
        this.notificationRepository = notificationRepository;
        this.paymentLogRepository = paymentLogRepository;
    }

    @Transactional
    public BookingResponse create(CreateBookingRequest request) {
        boolean alreadyBooked = bookingRepository.existsByRideRideIdAndSeatNumberAndBookingStatusIn(
                request.rideId(), request.seatNumber(), List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED));
        if (alreadyBooked) {
            throw new IllegalArgumentException("Seat already booked for this ride");
        }

        User user = userRepository.findById(request.userId()).orElseThrow(() -> new IllegalArgumentException("User not found"));
        Ride ride = rideRepository.findById(request.rideId()).orElseThrow(() -> new IllegalArgumentException("Ride not found"));
        if (!(ride.getRideStatus() == RideStatus.ACTIVE || ride.getRideStatus() == RideStatus.FULL)) {
            throw new IllegalArgumentException("Ride is not open for booking");
        }
        if (ride.getAvailableSeats() == null || ride.getAvailableSeats() <= 0) {
            throw new IllegalArgumentException("No seats available for this ride");
        }
        BoardingPoint pickup = boardingPointRepository
                .findById(request.pickupPointId())
                .orElseThrow(() -> new IllegalArgumentException("Pickup point not found"));
        BoardingPoint drop = boardingPointRepository
                .findById(request.dropPointId())
                .orElseThrow(() -> new IllegalArgumentException("Drop point not found"));

        Booking booking = new Booking();
        booking.setUser(user);
        booking.setRide(ride);
        booking.setPickupPoint(pickup);
        booking.setDropPoint(drop);
        booking.setSeatNumber(request.seatNumber());
        booking.setBookingTime(Instant.now());
        booking.setBookingStatus(BookingStatus.CONFIRMED);
        Booking saved = bookingRepository.save(booking);
        ride.setAvailableSeats(Math.max(0, ride.getAvailableSeats() - 1));
        ride.setRideStatus(ride.getAvailableSeats() == 0 ? RideStatus.FULL : RideStatus.ACTIVE);
        rideRepository.save(ride);
        String paymentMethod = normalizePaymentMethod(request.paymentMethod());
        createPaymentLog(saved, 350, "SUCCESS", paymentMethod);
        createAlert(user, "Booking Confirmed", "Your booking #" + saved.getBookingId() + " is confirmed.");
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public BookingResponse getById(Long bookingId) {
        return toResponse(bookingRepository.findById(bookingId).orElseThrow(() -> new IllegalArgumentException("Booking not found")));
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> getUserBookings(Long userId) {
        return bookingRepository.findByUserUserIdOrderByBookingTimeDesc(userId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public BookingResponse cancel(Long bookingId, String reason, CancelledBy cancelledBy) {
        Booking booking = bookingRepository.findById(bookingId).orElseThrow(() -> new IllegalArgumentException("Booking not found"));
        if (booking.getBookingStatus() == BookingStatus.CANCELLED) {
            throw new IllegalArgumentException("Booking is already cancelled");
        }
        booking.setBookingStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);
        Ride ride = booking.getRide();
        if (ride != null && ride.getRideStatus() != RideStatus.COMPLETED && ride.getRideStatus() != RideStatus.CANCELLED) {
            int nextSeats = (ride.getAvailableSeats() == null ? 0 : ride.getAvailableSeats()) + 1;
            ride.setAvailableSeats(nextSeats);
            ride.setRideStatus(RideStatus.ACTIVE);
            rideRepository.save(ride);
        }

        Cancellation cancellation = new Cancellation();
        cancellation.setBooking(booking);
        cancellation.setCancelledBy(cancelledBy);
        cancellation.setReason(reason);
        cancellation.setCancelledAt(Instant.now());
        cancellationRepository.save(cancellation);
        createPaymentLog(booking, -250, "REFUND_PROCESSED", "WALLET");
        createAlert(
                booking.getUser(),
                "Booking Cancelled",
                "Your booking #" + booking.getBookingId() + " has been cancelled. Reason: " + reason);
        return toResponse(booking);
    }

    private void createAlert(User user, String title, String message) {
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setType(NotificationType.ALERT);
        notification.setRead(Boolean.FALSE);
        notification.setCreatedAt(Instant.now());
        notificationRepository.save(notification);
    }

    private BookingResponse toResponse(Booking booking) {
        return new BookingResponse(
                booking.getBookingId(),
                booking.getUser().getUserId(),
                booking.getRide().getRideId(),
                booking.getSeatNumber(),
                booking.getBookingStatus(),
                booking.getBookingTime(),
                booking.getRide().getRideStatus().name());
    }

    private void createPaymentLog(Booking booking, int amount, String status, String method) {
        PaymentLog paymentLog = new PaymentLog();
        paymentLog.setBooking(booking);
        paymentLog.setUser(booking.getUser());
        paymentLog.setRide(booking.getRide());
        paymentLog.setAmount(amount);
        paymentLog.setStatus(status);
        paymentLog.setMethod(method);
        paymentLog.setReferenceCode("PAY-" + booking.getBookingId() + "-" + Instant.now().toEpochMilli());
        paymentLog.setCreatedAt(Instant.now());
        paymentLogRepository.save(paymentLog);
    }

    private String normalizePaymentMethod(String method) {
        if (method == null || method.trim().isEmpty()) return "DUMMY";
        return method.trim().toUpperCase();
    }
}
