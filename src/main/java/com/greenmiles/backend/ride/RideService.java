package com.greenmiles.backend.ride;

import com.greenmiles.backend.ride.dto.RideSearchResponse;
import com.greenmiles.backend.ride.dto.CityResponse;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RideService {

    private final RideRepository rideRepository;
    private final CityRepository cityRepository;

    public RideService(RideRepository rideRepository, CityRepository cityRepository) {
        this.rideRepository = rideRepository;
        this.cityRepository = cityRepository;
    }

    @Transactional(readOnly = true)
    public List<RideSearchResponse> search(Long startCityId, Long endCityId, LocalDate date) {
        LocalDateTime from = date.atStartOfDay();
        LocalDateTime to = date.plusDays(1).atStartOfDay().minusNanos(1);

        return rideRepository
                .searchRides(startCityId, endCityId, List.of(RideStatus.ACTIVE, RideStatus.FULL), from, to)
                .stream()
                .map(ride -> new RideSearchResponse(
                        ride.getRideId(),
                        ride.getStartCity().getCityName(),
                        ride.getEndCity().getCityName(),
                        ride.getStartTime(),
                        ride.getRideStatus(),
                        ride.getAvailableSeats()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CityResponse> getCities() {
        return cityRepository.findAll().stream()
                .map(c -> new CityResponse(c.getCityId(), c.getCityName()))
                .toList();
    }
}
