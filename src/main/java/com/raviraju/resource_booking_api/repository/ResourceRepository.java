package com.raviraju.resource_booking_api.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.raviraju.resource_booking_api.entity.Resource;
import com.raviraju.resource_booking_api.entity.ResourceType;

public interface ResourceRepository extends JpaRepository<Resource, Long> {

    List<Resource> findByType(ResourceType type);

    List<Resource> findByAvailable(boolean available);

    List<Resource> findByTypeAndAvailable(ResourceType type, boolean available);
}
