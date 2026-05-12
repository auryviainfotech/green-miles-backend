package com.greenmiles.backend.admin;

import com.greenmiles.backend.booking.Booking;
import com.greenmiles.backend.ride.Ride;
import com.greenmiles.backend.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "payment_logs")
public class PaymentLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_log_id")
    private Long paymentLogId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ride_id", nullable = false)
    private Ride ride;

    @Column(name = "amount", nullable = false)
    private Integer amount;

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @Column(name = "method", nullable = false, length = 30)
    private String method;

    @Column(name = "reference_code", nullable = false, length = 80)
    private String referenceCode;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public Long getPaymentLogId() {
        return paymentLogId;
    }

    public Booking getBooking() {
        return booking;
    }

    public User getUser() {
        return user;
    }

    public Ride getRide() {
        return ride;
    }

    public Integer getAmount() {
        return amount;
    }

    public String getStatus() {
        return status;
    }

    public String getMethod() {
        return method;
    }

    public String getReferenceCode() {
        return referenceCode;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setBooking(Booking booking) {
        this.booking = booking;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public void setRide(Ride ride) {
        this.ride = ride;
    }

    public void setAmount(Integer amount) {
        this.amount = amount;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public void setReferenceCode(String referenceCode) {
        this.referenceCode = referenceCode;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
