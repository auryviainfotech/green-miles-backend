package com.greenmiles.backend.ride;

import com.greenmiles.backend.common.ApiResponse;
import com.greenmiles.backend.ride.dto.CityResponse;
import com.greenmiles.backend.ride.dto.RideSearchResponse;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/rides")
public class RideController {

    private final RideService rideService;

    public RideController(RideService rideService) {
        this.rideService = rideService;
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<RideSearchResponse>>> search(
            @RequestParam Long startCityId,
            @RequestParam Long endCityId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<RideSearchResponse> rides = rideService.search(startCityId, endCityId, date);
        return ResponseEntity.ok(ApiResponse.ok("Rides fetched", rides));
    }

    @GetMapping("/cities")
    public ResponseEntity<ApiResponse<List<CityResponse>>> cities() {
        return ResponseEntity.ok(ApiResponse.ok("Cities fetched", rideService.getCities()));
    }
}
