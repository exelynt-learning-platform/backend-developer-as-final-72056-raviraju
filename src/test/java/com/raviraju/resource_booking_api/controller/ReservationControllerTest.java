package com.raviraju.resource_booking_api.controller;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.raviraju.resource_booking_api.dto.PageResponse;
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

    @MockBean
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
                .status(ReservationStatus.PENDING)
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
    void getReservations_Success() throws Exception {
        ReservationResponse res = ReservationResponse.builder()
                .id(10L)
                .resourceName("Meeting Room")
                .username("alice")
                .status(ReservationStatus.PENDING)
                .build();

        PageResponse<ReservationResponse> pageResponse = PageResponse.of(new PageImpl<>(List.of(res)));
        when(reservationService.getReservations(eq("alice"), any(), any(), any(), any(Pageable.class)))
                .thenReturn(pageResponse);

        mockMvc.perform(get("/api/reservations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(10L));
    }

    @Test
    @WithMockUser(username = "alice", roles = "USER")
    void getReservationById_Success() throws Exception {
        ReservationResponse res = ReservationResponse.builder()
                .id(10L)
                .resourceName("Meeting Room")
                .username("alice")
                .status(ReservationStatus.PENDING)
                .build();

        when(reservationService.getReservationById(eq(10L), eq("alice"))).thenReturn(res);

        mockMvc.perform(get("/api/reservations/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10L));
    }

    @Test
    @WithMockUser(username = "alice", roles = "USER")
    void cancelReservation_Success() throws Exception {
        ReservationResponse res = ReservationResponse.builder()
                .id(10L)
                .status(ReservationStatus.CANCELLED)
                .build();

        when(reservationService.cancelReservation(eq(10L), eq("alice"))).thenReturn(res);

        mockMvc.perform(patch("/api/reservations/10/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
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

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void deleteReservation_AsAdmin_Success() throws Exception {
        doNothing().when(reservationService).deleteReservation(10L);

        mockMvc.perform(delete("/api/reservations/10"))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "alice", roles = "USER")
    void getReservations_InvalidSortProperty_ReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/reservations?sort=user.password,desc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }
}
