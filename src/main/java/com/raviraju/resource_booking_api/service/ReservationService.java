package com.raviraju.resource_booking_api.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.raviraju.resource_booking_api.dto.PageResponse;
import com.raviraju.resource_booking_api.dto.ReservationRequest;
import com.raviraju.resource_booking_api.dto.ReservationResponse;
import com.raviraju.resource_booking_api.entity.Reservation;
import com.raviraju.resource_booking_api.entity.ReservationStatus;
import com.raviraju.resource_booking_api.entity.Resource;
import com.raviraju.resource_booking_api.entity.Role;
import com.raviraju.resource_booking_api.entity.User;
import com.raviraju.resource_booking_api.exception.BadRequestException;
import com.raviraju.resource_booking_api.exception.ResourceConflictException;
import com.raviraju.resource_booking_api.exception.ResourceNotFoundException;
import com.raviraju.resource_booking_api.repository.ReservationRepository;
import com.raviraju.resource_booking_api.repository.UserRepository;

import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final ResourceService resourceService;
    private final UserRepository userRepository;

    @Transactional
    public ReservationResponse createReservation(ReservationRequest request, String username) {
        User user = resolveUser(username);

        // Acquire pessimistic lock on the resource row to prevent concurrent race conditions
        Resource resource = resourceService.findResourceEntityByIdForUpdate(request.getResourceId());
        if (!resource.isAvailable()) {
            throw new BadRequestException("Resource '" + resource.getName() + "' is currently not available for booking.");
        }

        // Check for conflicting bookings in the requested time window
        List<Reservation> conflicts = reservationRepository.findOverlappingReservations(
                resource.getId(), request.getStartTime(), request.getEndTime());
        if (!conflicts.isEmpty()) {
            throw new ResourceConflictException("Resource is already booked during the requested time window (" +
                    request.getStartTime() + " to " + request.getEndTime() + ").");
        }

        Reservation reservation = Reservation.builder()
                .resource(resource)
                .user(user)
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .price(request.getPrice())
                .status(ReservationStatus.PENDING)
                .build();

        Reservation saved = reservationRepository.save(reservation);
        return ReservationResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public PageResponse<ReservationResponse> getReservations(
            String username,
            ReservationStatus status,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Pageable pageable
    ) {
        User user = resolveUser(username);
        Specification<Reservation> spec = buildReservationSpec(user, status, minPrice, maxPrice);

        Page<Reservation> page = reservationRepository.findAll(spec, pageable);
        Page<ReservationResponse> responsePage = page.map(ReservationResponse::fromEntity);
        return PageResponse.of(responsePage);
    }

    @Transactional(readOnly = true)
    public ReservationResponse getReservationById(Long id, String username) {
        User currentUser = resolveUser(username);

        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found with id: " + id));

        if (currentUser.getRole() != Role.ADMIN && !reservation.getUser().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("You do not have permission to view this reservation.");
        }

        return ReservationResponse.fromEntity(reservation);
    }

    @Transactional
    public ReservationResponse updateReservationStatus(Long id, ReservationStatus newStatus, String username) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found with id: " + id));

        log.info("Admin '{}' updated status of reservation ID {} to {}", username, id, newStatus);
        reservation.setStatus(newStatus);
        Reservation updated = reservationRepository.save(reservation);
        return ReservationResponse.fromEntity(updated);
    }

    @Transactional
    public ReservationResponse cancelReservation(Long id, String username) {
        User currentUser = resolveUser(username);

        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found with id: " + id));

        if (currentUser.getRole() != Role.ADMIN && !reservation.getUser().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("You do not have permission to cancel this reservation.");
        }

        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new BadRequestException("Reservation is already cancelled.");
        }

        reservation.setStatus(ReservationStatus.CANCELLED);
        Reservation updated = reservationRepository.save(reservation);
        return ReservationResponse.fromEntity(updated);
    }

    @Transactional
    public void deleteReservation(Long id) {
        if (!reservationRepository.existsById(id)) {
            throw new ResourceNotFoundException("Reservation not found with id: " + id);
        }
        reservationRepository.deleteById(id);
    }

    private User resolveUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
    }

    private Specification<Reservation> buildReservationSpec(
            User user,
            ReservationStatus status,
            BigDecimal minPrice,
            BigDecimal maxPrice
    ) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Non-admin users can only view their own reservations
            if (user.getRole() != Role.ADMIN) {
                predicates.add(criteriaBuilder.equal(root.get("user"), user));
            }

            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }

            if (minPrice != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("price"), minPrice));
            }

            if (maxPrice != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("price"), maxPrice));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
