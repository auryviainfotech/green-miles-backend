package com.greenmiles.backend.admin;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.greenmiles.backend.admin.dto.AdminAuditLogPageResponse;
import com.greenmiles.backend.admin.dto.AdminAuditLogResponse;
import com.greenmiles.backend.admin.dto.AdminDashboardResponse;
import com.greenmiles.backend.admin.dto.AdminDriverResponse;
import com.greenmiles.backend.admin.dto.PaymentLogResponse;
import com.greenmiles.backend.admin.dto.RideAssignmentResponse;
import com.greenmiles.backend.admin.dto.RoutePlanResponse;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class AdminControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminService adminService;

    @Test
    void dashboardReturnsDataForAdminPrincipal() throws Exception {
        when(adminService.getDashboard()).thenReturn(new AdminDashboardResponse(
                12, 4, 18, 6, 45, 7, 3, 5, 9, 8, 1, 25200));

        mockMvc.perform(get("/api/v1/admin/dashboard")
                        .with(user("ADMIN:admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalUsers").value(12))
                .andExpect(jsonPath("$.data.totalPaymentAmount").value(25200));
    }

    @Test
    void adminEndpointsRejectNonAdminPrincipal() throws Exception {
        mockMvc.perform(get("/api/v1/admin/dashboard")
                        .with(user("USER:1").roles("USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void listDriversReturnsDriverRows() throws Exception {
        when(adminService.listDrivers()).thenReturn(List.of(new AdminDriverResponse(
                1L,
                "Driver One",
                "9001001001",
                "LIC001",
                "driver1@greenmiles.in",
                "Hyderabad",
                "VID001",
                "ACTIVE",
                4.8,
                11L,
                "TS09AB1234",
                "RC001",
                "KIA_CARENS_7_SEATER",
                7,
                "ACTIVE")));

        mockMvc.perform(get("/api/v1/admin/drivers")
                        .with(user("ADMIN:admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].driverId").value(1))
                .andExpect(jsonPath("$.data[0].carId").value(11));
    }

    @Test
    void createDriverCreatesNewDriverResponse() throws Exception {
        when(adminService.createDriver(any())).thenReturn(new AdminDriverResponse(
                5L,
                "Smoke Driver",
                "9009988888",
                "LICSMK11",
                "smoke@greenmiles.in",
                "Hyd",
                "VID11",
                "ACTIVE",
                4.5,
                55L,
                "TS09SM1111",
                "RCSM1111",
                "KIA_CARENS_7_SEATER",
                7,
                "ACTIVE"));

        String body = """
                {
                  "fullName": "Smoke Driver",
                  "phone": "9009988888",
                  "email": "smoke@greenmiles.in",
                  "address": "Hyd",
                  "vidProofNumber": "VID11",
                  "licenseNumber": "LICSMK11",
                  "status": "ACTIVE",
                  "rating": 4.5,
                  "vehicleNumber": "TS09SM1111",
                  "rcNumber": "RCSM1111",
                  "vehicleType": "KIA_CARENS_7_SEATER",
                  "totalSeats": 7,
                  "carStatus": "ACTIVE"
                }
                """;

        mockMvc.perform(post("/api/v1/admin/drivers")
                        .with(user("ADMIN:admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.driverId").value(5))
                .andExpect(jsonPath("$.data.carId").value(55));
    }

    @Test
    void createRouteReturnsRoutePlan() throws Exception {
        when(adminService.createRoute(any())).thenReturn(new RoutePlanResponse(
                8L, "HYD-MAC-1", 1L, "Hyderabad", 2L, "Macherla", 450, 6, true, Instant.now()));

        String body = """
                {
                  "startCityId": 1,
                  "endCityId": 2,
                  "baseFare": 450,
                  "defaultSeats": 6
                }
                """;

        mockMvc.perform(post("/api/v1/admin/routes")
                        .with(user("ADMIN:admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.routePlanId").value(8))
                .andExpect(jsonPath("$.data.startCityName").value("Hyderabad"));
    }

    @Test
    void assignRideReturnsAssignmentResponse() throws Exception {
        when(adminService.assignRide(any())).thenReturn(new RideAssignmentResponse(
                19L,
                8L,
                "HYD-MAC-1",
                1L,
                "Driver One",
                11L,
                "TS09AB1234",
                "Hyderabad",
                "Macherla",
                LocalDateTime.of(2026, 4, 21, 9, 30),
                "ACTIVE",
                4));

        String body = """
                {
                  "routePlanId": 8,
                  "driverId": 1,
                  "carId": 11,
                  "startTime": "2026-04-21T09:30:00",
                  "availableSeats": 4
                }
                """;

        mockMvc.perform(post("/api/v1/admin/ride-assignments")
                        .with(user("ADMIN:admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.rideId").value(19))
                .andExpect(jsonPath("$.data.routeCode").value("HYD-MAC-1"));
    }

    @Test
    void paymentLogsAndAuditLogsReturnPagedData() throws Exception {
        when(adminService.listPaymentLogs()).thenReturn(List.of(new PaymentLogResponse(
                101L, 44L, 2L, 7L, 550, "SUCCESS", "UPI", "REF-101", Instant.now())));
        when(adminService.getDeniedAuditLogs(anyInt(), anyInt(), anyString()))
                .thenReturn(new AdminAuditLogPageResponse(
                        List.of(new AdminAuditLogResponse(
                                5L, 2L, "ACCESS_DENIED", "ADMIN", 44L, "Denied", Instant.now())),
                        0,
                        5,
                        1,
                        1));

        mockMvc.perform(get("/api/v1/admin/payments")
                        .with(user("ADMIN:admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].paymentLogId").value(101))
                .andExpect(jsonPath("$.data[0].status").value("SUCCESS"));

        mockMvc.perform(get("/api/v1/admin/audit-logs?page=0&size=5&entity=ADMIN")
                        .with(user("ADMIN:admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items[0].action").value("ACCESS_DENIED"))
                .andExpect(jsonPath("$.data.totalItems").value(1));
    }
}

