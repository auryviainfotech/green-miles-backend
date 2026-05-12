package com.greenmiles.backend.driver;

import com.greenmiles.backend.audit.AuditLog;
import com.greenmiles.backend.audit.AuditLogRepository;
import com.greenmiles.backend.driver.dto.DriverApplicationRequest;
import com.greenmiles.backend.driver.dto.DriverApplicationResponse;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DriverApplicationService {

    private static final String ENTITY = "DRIVER_APPLICATION";
    private final AuditLogRepository auditLogRepository;
    private final DriverRepository driverRepository;

    public DriverApplicationService(AuditLogRepository auditLogRepository, DriverRepository driverRepository) {
        this.auditLogRepository = auditLogRepository;
        this.driverRepository = driverRepository;
    }

    @Transactional
    public DriverApplicationResponse submit(DriverApplicationRequest request) {
        if (!driverRepository.existsById(request.driverId())) {
            throw new IllegalArgumentException("Driver not found");
        }
        AuditLog log = new AuditLog();
        log.setUserId(null);
        log.setAction("SUBMITTED");
        log.setEntity(ENTITY);
        log.setEntityId(request.driverId());
        Map<String, Object> payload = new HashMap<>();
        payload.put("driverId", request.driverId());
        payload.put("fullName", request.fullName());
        payload.put("mobile", request.mobile());
        payload.put("email", request.email());
        payload.put("vehicleModel", request.vehicleModel());
        payload.put("vehicleRegistration", request.vehicleRegistration());
        payload.put("carRcNumber", request.carRcNumber());
        payload.put("driverLicenseNumber", request.driverLicenseNumber());
        payload.put("pollutionCertificateNumber", request.pollutionCertificateNumber());
        payload.put("roadTaxReceiptNumber", request.roadTaxReceiptNumber());
        payload.put("permitNumber", request.permitNumber());
        payload.put("insurancePolicyNumber", request.insurancePolicyNumber());
        payload.put("status", "PENDING");
        log.setDetails(writeJson(payload));
        log.setCreatedAt(Instant.now());
        return toResponse(auditLogRepository.save(log));
    }

    @Transactional(readOnly = true)
    public List<DriverApplicationResponse> list(String status) {
        List<AuditLog> logs = auditLogRepository.findByEntityOrderByCreatedAtDesc(ENTITY);
        List<DriverApplicationResponse> out = new ArrayList<>();
        String wanted = status == null ? null : status.trim().toUpperCase();
        for (AuditLog log : logs) {
            DriverApplicationResponse item = toResponse(log);
            if (wanted == null || wanted.isBlank() || wanted.equals(item.status())) {
                out.add(item);
            }
        }
        return out;
    }

    @Transactional
    public DriverApplicationResponse updateStatus(Long applicationId, String status, String reviewedBy) {
        String normalized = status.trim().toUpperCase();
        if (!List.of("PENDING", "APPROVED", "REJECTED").contains(normalized)) {
            throw new IllegalArgumentException("Invalid status");
        }
        AuditLog log = auditLogRepository
                .findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Application not found"));
        if (!ENTITY.equals(log.getEntity())) {
            throw new IllegalArgumentException("Invalid application entity");
        }
        Map<String, Object> details = parseDetails(log.getDetails());
        details.put("status", normalized);
        details.put("reviewedBy", reviewedBy);
        details.put("reviewedAt", Instant.now().toString());
        log.setAction("REVIEWED");
        log.setDetails(writeJson(details));
        return toResponse(auditLogRepository.save(log));
    }

    private DriverApplicationResponse toResponse(AuditLog log) {
        Map<String, Object> d = parseDetails(log.getDetails());
        return new DriverApplicationResponse(
                log.getLogId(),
                asLong(d.get("driverId"), log.getEntityId()),
                asString(d.get("fullName")),
                asString(d.get("email")),
                asString(d.get("mobile")),
                asString(d.get("vehicleModel")),
                asString(d.get("vehicleRegistration")),
                asStringOrDefault(d.get("status"), "PENDING"),
                asString(d.get("reviewedBy")),
                log.getCreatedAt());
    }

    private String writeJson(Map<String, Object> payload) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : payload.entrySet()) {
            if (!first) sb.append(",");
            first = false;
            sb.append("\"").append(entry.getKey()).append("\":");
            Object value = entry.getValue();
            if (value instanceof Number || value instanceof Boolean) {
                sb.append(value);
            } else {
                sb.append("\"").append(escape(String.valueOf(value))).append("\"");
            }
        }
        sb.append("}");
        return sb.toString();
    }

    private Map<String, Object> parseDetails(String details) {
        Map<String, Object> out = new HashMap<>();
        if (details == null || details.isBlank()) return out;
        parseStringField(details, "fullName", out);
        parseStringField(details, "mobile", out);
        parseStringField(details, "email", out);
        parseStringField(details, "vehicleModel", out);
        parseStringField(details, "vehicleRegistration", out);
        parseStringField(details, "carRcNumber", out);
        parseStringField(details, "driverLicenseNumber", out);
        parseStringField(details, "pollutionCertificateNumber", out);
        parseStringField(details, "roadTaxReceiptNumber", out);
        parseStringField(details, "permitNumber", out);
        parseStringField(details, "insurancePolicyNumber", out);
        parseStringField(details, "status", out);
        parseStringField(details, "reviewedBy", out);
        parseLongField(details, "driverId", out);
        return out;
    }

    private void parseStringField(String json, String key, Map<String, Object> out) {
        Pattern p = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\"([^\"]*)\"");
        Matcher m = p.matcher(json);
        if (m.find()) out.put(key, m.group(1));
    }

    private void parseLongField(String json, String key, Map<String, Object> out) {
        Pattern p = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*(\\d+)");
        Matcher m = p.matcher(json);
        if (m.find()) out.put(key, Long.parseLong(m.group(1)));
    }

    private String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
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
}
