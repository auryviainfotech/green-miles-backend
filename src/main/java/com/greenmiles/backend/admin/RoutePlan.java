package com.greenmiles.backend.admin;

import com.greenmiles.backend.ride.City;
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
@Table(name = "route_plans")
public class RoutePlan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "route_plan_id")
    private Long routePlanId;

    @Column(name = "route_code", nullable = false, unique = true, length = 40)
    private String routeCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "start_city_id", nullable = false)
    private City startCity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "end_city_id", nullable = false)
    private City endCity;

    @Column(name = "base_fare", nullable = false)
    private Integer baseFare;

    @Column(name = "default_seats", nullable = false)
    private Integer defaultSeats;

    @Column(name = "is_active", nullable = false)
    private Boolean active;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public Long getRoutePlanId() {
        return routePlanId;
    }

    public String getRouteCode() {
        return routeCode;
    }

    public City getStartCity() {
        return startCity;
    }

    public City getEndCity() {
        return endCity;
    }

    public Integer getBaseFare() {
        return baseFare;
    }

    public Integer getDefaultSeats() {
        return defaultSeats;
    }

    public Boolean getActive() {
        return active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setRouteCode(String routeCode) {
        this.routeCode = routeCode;
    }

    public void setStartCity(City startCity) {
        this.startCity = startCity;
    }

    public void setEndCity(City endCity) {
        this.endCity = endCity;
    }

    public void setBaseFare(Integer baseFare) {
        this.baseFare = baseFare;
    }

    public void setDefaultSeats(Integer defaultSeats) {
        this.defaultSeats = defaultSeats;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
