package com.greenmiles.backend.ride;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "cars")
public class Car {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "car_id")
    private Long carId;

    @Column(name = "driver_id", nullable = false)
    private Long driverId;

    @Column(name = "vehicle_number", nullable = false)
    private String vehicleNumber;

    @Column(name = "rc_number", nullable = false)
    private String rcNumber;

    @Column(name = "vehicle_type", nullable = false)
    private String vehicleType;

    @Column(name = "total_seats", nullable = false)
    private Integer totalSeats;

    @Column(name = "status", nullable = false)
    private String status;

    public Long getCarId() {
        return carId;
    }

    public Long getDriverId() {
        return driverId;
    }

    public String getVehicleNumber() {
        return vehicleNumber;
    }

    public String getRcNumber() {
        return rcNumber;
    }

    public String getVehicleType() {
        return vehicleType;
    }

    public Integer getTotalSeats() {
        return totalSeats;
    }

    public String getStatus() {
        return status;
    }

    public void setDriverId(Long driverId) {
        this.driverId = driverId;
    }

    public void setVehicleNumber(String vehicleNumber) {
        this.vehicleNumber = vehicleNumber;
    }

    public void setRcNumber(String rcNumber) {
        this.rcNumber = rcNumber;
    }

    public void setVehicleType(String vehicleType) {
        this.vehicleType = vehicleType;
    }

    public void setTotalSeats(Integer totalSeats) {
        this.totalSeats = totalSeats;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
