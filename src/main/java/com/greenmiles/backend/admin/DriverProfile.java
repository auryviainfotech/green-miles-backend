package com.greenmiles.backend.admin;

import com.greenmiles.backend.driver.Driver;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "driver_profiles")
public class DriverProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "driver_profile_id")
    private Long driverProfileId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id", nullable = false, unique = true)
    private Driver driver;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "address", length = 500)
    private String address;

    @Column(name = "vid_proof_number", length = 120)
    private String vidProofNumber;

    public Long getDriverProfileId() {
        return driverProfileId;
    }

    public Driver getDriver() {
        return driver;
    }

    public String getEmail() {
        return email;
    }

    public String getAddress() {
        return address;
    }

    public String getVidProofNumber() {
        return vidProofNumber;
    }

    public void setDriver(Driver driver) {
        this.driver = driver;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void setVidProofNumber(String vidProofNumber) {
        this.vidProofNumber = vidProofNumber;
    }
}
