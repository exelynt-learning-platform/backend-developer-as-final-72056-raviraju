package com.raviraju.resource_booking_api.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.raviraju.resource_booking_api.entity.Reservation;

import jakarta.persistence.LockModeType;

public interface ReservationRepository extends JpaRepository<Reservation, Long>, JpaSpecificationExecutor<Reservation> {

    @Override
    @EntityGraph(attributePaths = {"resource", "user"})
    Page<Reservation> findAll(Specification<Reservation> spec, Pageable pageable);

    // Overlap condition: existing.start < new.end AND existing.end > new.start
    // Pessimistic write lock prevents concurrent double bookings
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Reservation r WHERE r.resource.id = :resourceId " +
           "AND r.status != com.raviraju.resource_booking_api.entity.ReservationStatus.CANCELLED " +
           "AND (r.startTime < :endTime AND r.endTime > :startTime)")
    List<Reservation> findOverlappingReservations(
            @Param("resourceId") Long resourceId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );
}
