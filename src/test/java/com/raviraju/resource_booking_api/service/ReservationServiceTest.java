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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;

import com.raviraju.resource_booking_api.dto.PageResponse;
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
import com.raviraju.resource_booking_api.exception.ResourceNotFoundException;
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
    private User adminUser;
    private User otherUser;
    private Resource resource;
    private LocalDateTime start;
    private LocalDateTime end;

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).username("testuser").role(Role.USER).build();
        adminUser = User.builder().id(2L).username("admin").role(Role.ADMIN).build();
        otherUser = User.builder().id(3L).username("other").role(Role.USER).build();
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
                .status(ReservationStatus.PENDING)
                .build();

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(resourceService.findResourceEntityById(10L)).thenReturn(resource);
        when(reservationRepository.findOverlappingReservations(10L, start, end)).thenReturn(Collections.emptyList());
        when(reservationRepository.save(any(Reservation.class))).thenReturn(savedReservation);

        ReservationResponse response = reservationService.createReservation(request, "testuser");

        assertNotNull(response);
        assertEquals(100L, response.getId());
        assertEquals(ReservationStatus.PENDING, response.getStatus());
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

    @Test
    void createReservation_InvalidTimes_ThrowsException() {
        ReservationRequest nullTimeRequest = ReservationRequest.builder()
                .resourceId(10L)
                .startTime(null)
                .endTime(end)
                .build();

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

        assertThrows(BadRequestException.class, () -> reservationService.createReservation(nullTimeRequest, "testuser"));

        ReservationRequest pastRequest = ReservationRequest.builder()
                .resourceId(10L)
                .startTime(LocalDateTime.now().minusDays(1))
                .endTime(LocalDateTime.now().plusDays(1))
                .build();

        assertThrows(BadRequestException.class, () -> reservationService.createReservation(pastRequest, "testuser"));

        ReservationRequest endBeforeStart = ReservationRequest.builder()
                .resourceId(10L)
                .startTime(LocalDateTime.now().plusDays(2))
                .endTime(LocalDateTime.now().plusDays(1))
                .build();

        assertThrows(BadRequestException.class, () -> reservationService.createReservation(endBeforeStart, "testuser"));
    }

    @Test
    void getReservations_Success() {
        Reservation res = Reservation.builder()
                .id(1L)
                .user(user)
                .resource(resource)
                .startTime(start)
                .endTime(end)
                .price(new BigDecimal("100.00"))
                .status(ReservationStatus.PENDING)
                .build();

        Page<Reservation> page = new PageImpl<>(List.of(res));
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(reservationRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        PageResponse<ReservationResponse> response = reservationService.getReservations(
                "testuser", ReservationStatus.PENDING, new BigDecimal("50"), new BigDecimal("200"), PageRequest.of(0, 10));

        assertNotNull(response);
        assertEquals(1, response.getContent().size());
    }

    @Test
    void getReservationById_Owner_Success() {
        Reservation res = Reservation.builder()
                .id(1L)
                .user(user)
                .resource(resource)
                .startTime(start)
                .endTime(end)
                .price(new BigDecimal("100.00"))
                .status(ReservationStatus.PENDING)
                .build();

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(reservationRepository.findById(1L)).thenReturn(Optional.of(res));

        ReservationResponse response = reservationService.getReservationById(1L, "testuser");

        assertNotNull(response);
        assertEquals(1L, response.getId());
    }

    @Test
    void getReservationById_Admin_Success() {
        Reservation res = Reservation.builder()
                .id(1L)
                .user(user)
                .resource(resource)
                .startTime(start)
                .endTime(end)
                .price(new BigDecimal("100.00"))
                .status(ReservationStatus.PENDING)
                .build();

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));
        when(reservationRepository.findById(1L)).thenReturn(Optional.of(res));

        ReservationResponse response = reservationService.getReservationById(1L, "admin");

        assertNotNull(response);
        assertEquals(1L, response.getId());
    }

    @Test
    void getReservationById_OtherUser_ThrowsAccessDenied() {
        Reservation res = Reservation.builder()
                .id(1L)
                .user(user)
                .resource(resource)
                .build();

        when(userRepository.findByUsername("other")).thenReturn(Optional.of(otherUser));
        when(reservationRepository.findById(1L)).thenReturn(Optional.of(res));

        assertThrows(AccessDeniedException.class, () -> reservationService.getReservationById(1L, "other"));
    }

    @Test
    void getReservationById_NotFound_ThrowsException() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(reservationRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> reservationService.getReservationById(99L, "testuser"));
    }

    @Test
    void updateReservationStatus_Admin_Success() {
        Reservation res = Reservation.builder()
                .id(1L)
                .user(user)
                .resource(resource)
                .status(ReservationStatus.PENDING)
                .build();

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));
        when(reservationRepository.findById(1L)).thenReturn(Optional.of(res));
        when(reservationRepository.save(any(Reservation.class))).thenReturn(res);

        ReservationResponse response = reservationService.updateReservationStatus(1L, ReservationStatus.CONFIRMED, "admin");

        assertNotNull(response);
        assertEquals(ReservationStatus.CONFIRMED, response.getStatus());
    }

    @Test
    void updateReservationStatus_NonAdmin_ThrowsAccessDenied() {
        Reservation res = Reservation.builder()
                .id(1L)
                .user(user)
                .resource(resource)
                .build();

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(reservationRepository.findById(1L)).thenReturn(Optional.of(res));

        assertThrows(AccessDeniedException.class, () -> reservationService.updateReservationStatus(1L, ReservationStatus.CONFIRMED, "testuser"));
    }

    @Test
    void cancelReservation_Owner_Success() {
        Reservation res = Reservation.builder()
                .id(1L)
                .user(user)
                .resource(resource)
                .status(ReservationStatus.PENDING)
                .build();

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(reservationRepository.findById(1L)).thenReturn(Optional.of(res));
        when(reservationRepository.save(any(Reservation.class))).thenReturn(res);

        ReservationResponse response = reservationService.cancelReservation(1L, "testuser");

        assertNotNull(response);
        assertEquals(ReservationStatus.CANCELLED, response.getStatus());
    }

    @Test
    void cancelReservation_AlreadyCancelled_ThrowsException() {
        Reservation res = Reservation.builder()
                .id(1L)
                .user(user)
                .resource(resource)
                .status(ReservationStatus.CANCELLED)
                .build();

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(reservationRepository.findById(1L)).thenReturn(Optional.of(res));

        assertThrows(BadRequestException.class, () -> reservationService.cancelReservation(1L, "testuser"));
    }

    @Test
    void cancelReservation_OtherUser_ThrowsAccessDenied() {
        Reservation res = Reservation.builder()
                .id(1L)
                .user(user)
                .resource(resource)
                .status(ReservationStatus.PENDING)
                .build();

        when(userRepository.findByUsername("other")).thenReturn(Optional.of(otherUser));
        when(reservationRepository.findById(1L)).thenReturn(Optional.of(res));

        assertThrows(AccessDeniedException.class, () -> reservationService.cancelReservation(1L, "other"));
    }

    @Test
    void deleteReservation_Success() {
        when(reservationRepository.existsById(1L)).thenReturn(true);

        reservationService.deleteReservation(1L);

        verify(reservationRepository).deleteById(1L);
    }

    @Test
    void deleteReservation_NotFound_ThrowsException() {
        when(reservationRepository.existsById(99L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> reservationService.deleteReservation(99L));
    }
}
