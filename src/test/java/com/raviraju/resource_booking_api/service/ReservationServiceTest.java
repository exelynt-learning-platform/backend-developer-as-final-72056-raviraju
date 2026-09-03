package com.raviraju.resource_booking_api.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.raviraju.resource_booking_api.dto.ReservationRequest;
import com.raviraju.resource_booking_api.dto.ReservationResponse;
import com.raviraju.resource_booking_api.entity.Reservation;
import com.raviraju.resource_booking_api.entity.ReservationStatus;
import com.raviraju.resource_booking_api.entity.Resource;
import com.raviraju.resource_booking_api.entity.ResourceType;
import com.raviraju.resource_booking_api.entity.Role;
import com.raviraju.resource_booking_api.entity.User;
import com.raviraju.resource_booking_api.exception.BadRequestException;
import com.raviraju.resource_booking_api.exception.ResourceConflictException;
import com.raviraju.resource_booking_api.repository.ReservationRepository;
import com.raviraju.resource_booking_api.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private ResourceService resourceService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ReservationService reservationService;

    private User user;
    private Resource resource;
    private LocalDateTime start;
    private LocalDateTime end;

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).username("testuser").role(Role.USER).build();
        resource = Resource.builder().id(10L).name("Room A").type(ResourceType.ROOM).available(true).build();
        start = LocalDateTime.now().plusDays(1);
        end = start.plusHours(2);
    }

    @Test
    void createReservation_Success() {
        ReservationRequest request = ReservationRequest.builder()
                .resourceId(10L)
                .startTime(start)
                .endTime(end)
                .price(new BigDecimal("150.00"))
                .build();

        Reservation savedReservation = Reservation.builder()
                .id(100L)
                .user(user)
                .resource(resource)
                .startTime(start)
                .endTime(end)
                .price(new BigDecimal("150.00"))
                .status(ReservationStatus.CONFIRMED)
                .build();

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(resourceService.findResourceEntityById(10L)).thenReturn(resource);
        when(reservationRepository.findOverlappingReservations(10L, start, end)).thenReturn(Collections.emptyList());
        when(reservationRepository.save(any(Reservation.class))).thenReturn(savedReservation);

        ReservationResponse response = reservationService.createReservation(request, "testuser");

        assertNotNull(response);
        assertEquals(100L, response.getId());
        assertEquals(ReservationStatus.CONFIRMED, response.getStatus());
    }

    @Test
    void createReservation_Conflict_ThrowsException() {
        ReservationRequest request = ReservationRequest.builder()
                .resourceId(10L)
                .startTime(start)
                .endTime(end)
                .price(new BigDecimal("150.00"))
                .build();

        Reservation existing = Reservation.builder().id(99L).build();

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(resourceService.findResourceEntityById(10L)).thenReturn(resource);
        when(reservationRepository.findOverlappingReservations(10L, start, end)).thenReturn(List.of(existing));

        assertThrows(ResourceConflictException.class, () -> reservationService.createReservation(request, "testuser"));
        verify(reservationRepository, never()).save(any());
    }

    @Test
    void createReservation_UnavailableResource_ThrowsException() {
        resource.setAvailable(false);
        ReservationRequest request = ReservationRequest.builder()
                .resourceId(10L)
                .startTime(start)
                .endTime(end)
                .price(new BigDecimal("150.00"))
                .build();

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(resourceService.findResourceEntityById(10L)).thenReturn(resource);

        assertThrows(BadRequestException.class, () -> reservationService.createReservation(request, "testuser"));
    }
}
