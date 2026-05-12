package com.greenmiles.backend.driver;

import com.greenmiles.backend.audit.AuditLog;
import com.greenmiles.backend.audit.AuditLogRepository;
import com.greenmiles.backend.driver.dto.RideApplicationRequest;
import com.greenmiles.backend.driver.dto.RideApplicationResponse;
import com.greenmiles.backend.ride.Car;
import com.greenmiles.backend.ride.CarRepository;
import com.greenmiles.backend.ride.City;
import com.greenmiles.backend.ride.CityRepository;
import com.greenmiles.backend.ride.Ride;
import com.greenmiles.backend.ride.RideEntityRepository;
import com.greenmiles.backend.ride.RideStatus;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RideApplicationService {

    private static final String ENTITY = "RIDE_APPLICATION";
    private final AuditLogRepository auditLogRepository;
    private final RideEntityRepository rideEntityRepository;
    private final CityRepository cityRepository;
    private final CarRepository carRepository;

    public RideApplicationService(
            AuditLogRepository auditLogRepository,
            RideEntityRepository rideEntityRepository,
            CityRepository cityRepository,
            CarRepository carRepository) {
        this.auditLogRepository = auditLogRepository;
        this.rideEntityRepository = rideEntityRepository;
        this.cityRepository = cityRepository;
        this.carRepository = carRepository;
    }

    @Transactional
    public RideApplicationResponse submit(RideApplicationRequest request) {
        AuditLog log = new AuditLog();
        log.setUserId(null);
        log.setAction("SUBMITTED");
        log.setEntity(ENTITY);
        log.setEntityId(request.driverId());
        Map<String, Object> payload = new HashMap<>();
        payload.put("driverId", request.driverId());
        payload.put("from", request.from());
        payload.put("to", request.to());
        payload.put("date", request.date());
        payload.put("time", request.time());
        payload.put("seats", request.seats());
        payload.put("price", request.price());
        payload.put("status", "PENDING");
        log.setDetails(writeJson(payload));
        log.setCreatedAt(Instant.now());
        return toResponse(auditLogRepository.save(log));
    }

    @Transactional(readOnly = true)
    public List<RideApplicationResponse> list(String status) {
        List<AuditLog> logs = auditLogRepository.findByEntityOrderByCreatedAtDesc(ENTITY);
        List<RideApplicationResponse> out = new ArrayList<>();
        String wanted = status == null ? null : status.trim().toUpperCase();
        for (AuditLog log : logs) {
            RideApplicationResponse item = toResponse(log);
            if (wanted == null || wanted.isBlank() || wanted.equals(item.status())) out.add(item);
        }
        return out;
    }

    @Transactional
    public RideApplicationResponse updateStatus(Long applicationId, String status) {
        String normalized = status.trim().toUpperCase();
        if (!List.of("PENDING", "APPROVED", "REJECTED").contains(normalized)) {
            throw new IllegalArgumentException("Invalid status");
        }
        AuditLog log = auditLogRepository.findById(applicationId).orElseThrow(() -> new IllegalArgumentException("Application not found"));
        if (!ENTITY.equals(log.getEntity())) throw new IllegalArgumentException("Invalid application entity");
        Map<String, Object> details = parseDetails(log.getDetails());
        details.put("status", normalized);

        if ("APPROVED".equals(normalized)) {
            Long driverId = asLong(details.get("driverId"), log.getEntityId());
            String from = asString(details.get("from"));
            String to = asString(details.get("to"));
            City startCity = cityRepository.findByCityNameIgnoreCase(from).orElseThrow(() -> new IllegalArgumentException("Unknown source city"));
            City endCity = cityRepository.findByCityNameIgnoreCase(to).orElseThrow(() -> new IllegalArgumentException("Unknown destination city"));
            Car car = carRepository.findFirstByDriverId(driverId).orElseThrow(() -> new IllegalArgumentException("No car mapped for driver"));
            String date = asString(details.get("date"));
            String time = asString(details.get("time"));
            LocalDateTime startTime = parseDateTime(date, time);
            Integer seats = asInt(details.get("seats"), 4);

            Ride ride = new Ride();
            ride.setDriverId(driverId);
            ride.setCarId(car.getCarId());
            ride.setStartCity(startCity);
            ride.setEndCity(endCity);
            ride.setStartTime(startTime);
            ride.setRideStatus(RideStatus.ACTIVE);
            ride.setAvailableSeats(seats);
            Ride saved = rideEntityRepository.save(ride);
            details.put("approvedRideId", saved.getRideId());
        }

        log.setAction("REVIEWED");
        log.setDetails(writeJson(details));
        return toResponse(auditLogRepository.save(log));
    }

    private LocalDateTime parseDateTime(String date, String time) {
        try {
            DateTimeFormatter f = DateTimeFormatter.ofPattern("yyyy-MM-dd h:mm a");
            return LocalDateTime.parse(date + " " + time.toUpperCase(), f);
        } catch (Exception ex) {
            return LocalDateTime.now().plusHours(2);
        }
    }

    private RideApplicationResponse toResponse(AuditLog log) {
        Map<String, Object> d = parseDetails(log.getDetails());
        return new RideApplicationResponse(
                log.getLogId(),
                asLong(d.get("driverId"), log.getEntityId()),
                asString(d.get("from")),
                asString(d.get("to")),
                asString(d.get("date")),
                asString(d.get("time")),
                asInt(d.get("seats"), 0),
                asInt(d.get("price"), 0),
                asStringOrDefault(d.get("status"), "PENDING"),
                asLong(d.get("approvedRideId"), null),
                log.getCreatedAt());
    }

    private String writeJson(Map<String, Object> payload) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> e : payload.entrySet()) {
            if (!first) sb.append(",");
            first = false;
            sb.append("\"").append(e.getKey()).append("\":");
            Object v = e.getValue();
            if (v == null) {
                sb.append("null");
            } else if (v instanceof Number || v instanceof Boolean) {
                sb.append(v);
            } else {
                sb.append("\"").append(String.valueOf(v).replace("\\", "\\\\").replace("\"", "\\\"")).append("\"");
            }
        }
        sb.append("}");
        return sb.toString();
    }

    private Map<String, Object> parseDetails(String json) {
        Map<String, Object> out = new HashMap<>();
        if (json == null || json.isBlank()) return out;
        for (String key : List.of("from", "to", "date", "time", "status")) parseStringField(json, key, out);
        for (String key : List.of("driverId", "seats", "price", "approvedRideId")) parseLongField(json, key, out);
        return out;
    }

    private void parseStringField(String json, String key, Map<String, Object> out) {
        Matcher m = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\"([^\"]*)\"").matcher(json);
        if (m.find()) out.put(key, m.group(1));
    }

    private void parseLongField(String json, String key, Map<String, Object> out) {
        Matcher m = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*(\\d+)").matcher(json);
        if (m.find()) out.put(key, Long.parseLong(m.group(1)));
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private String asStringOrDefault(Object value, String defaultValue) {
        return value == null ? defaultValue : String.valueOf(value);
    }

    private Long asLong(Object value, Long fallback) {
        if (value == null) return fallback;
        if (value instanceof Number n) return n.longValue();
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (Exception ex) {
            return fallback;
        }
    }

    private Integer asInt(Object value, Integer fallback) {
        if (value == null) return fallback;
        if (value instanceof Number n) return n.intValue();
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception ex) {
            return fallback;
        }
    }
}
