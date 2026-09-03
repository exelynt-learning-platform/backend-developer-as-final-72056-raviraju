package com.raviraju.resource_booking_api.controller;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.raviraju.resource_booking_api.dto.ReservationRequest;
import com.raviraju.resource_booking_api.dto.ReservationResponse;
import com.raviraju.resource_booking_api.dto.ReservationStatusUpdateRequest;
import com.raviraju.resource_booking_api.entity.ReservationStatus;
import com.raviraju.resource_booking_api.service.ReservationService;

@SpringBootTest
@AutoConfigureMockMvc
class ReservationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @MockitoBean
    private ReservationService reservationService;

    @Test
    @WithMockUser(username = "alice", roles = "USER")
    void createReservation_Success() throws Exception {
        ReservationRequest request = ReservationRequest.builder()
                .resourceId(1L)
                .startTime(LocalDateTime.now().plusDays(1))
                .endTime(LocalDateTime.now().plusDays(1).plusHours(2))
                .price(new BigDecimal("100.00"))
                .build();

        ReservationResponse response = ReservationResponse.builder()
                .id(10L)
                .resourceId(1L)
                .resourceName("Meeting Room")
                .userId(5L)
                .username("alice")
                .status(ReservationStatus.CONFIRMED)
                .build();

        when(reservationService.createReservation(any(ReservationRequest.class), eq("alice"))).thenReturn(response);

        String requestJson = """
                {
                    "resourceId": 1,
                    "startTime": "2028-10-01T10:00:00",
                    "endTime": "2028-10-01T12:00:00",
                    "price": 100.00
                }
                """;

        mockMvc.perform(post("/api/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10L))
                .andExpect(jsonPath("$.username").value("alice"));
    }

    @Test
    @WithMockUser(username = "alice", roles = "USER")
    void updateReservationStatus_AsUser_Forbidden() throws Exception {
        ReservationStatusUpdateRequest request = new ReservationStatusUpdateRequest(ReservationStatus.CONFIRMED);

        mockMvc.perform(patch("/api/reservations/10/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void updateReservationStatus_AsAdmin_Success() throws Exception {
        ReservationStatusUpdateRequest request = new ReservationStatusUpdateRequest(ReservationStatus.CONFIRMED);

        ReservationResponse response = ReservationResponse.builder()
                .id(10L)
                .status(ReservationStatus.CONFIRMED)
                .build();

        when(reservationService.updateReservationStatus(eq(10L), eq(ReservationStatus.CONFIRMED), eq("admin")))
                .thenReturn(response);

        mockMvc.perform(patch("/api/reservations/10/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }
}
