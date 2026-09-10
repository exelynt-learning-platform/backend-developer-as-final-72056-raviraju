package com.raviraju.resource_booking_api.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.raviraju.resource_booking_api.entity.Resource;
import com.raviraju.resource_booking_api.entity.ResourceType;

import jakarta.persistence.LockModeType;

public interface ResourceRepository extends JpaRepository<Resource, Long> {

    List<Resource> findByType(ResourceType type);

    List<Resource> findByAvailable(boolean available);

    List<Resource> findByTypeAndAvailable(ResourceType type, boolean available);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Resource r WHERE r.id = :id")
    Optional<Resource> findByIdForUpdate(@Param("id") Long id);
}
