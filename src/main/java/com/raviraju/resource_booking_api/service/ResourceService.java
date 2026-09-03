package com.raviraju.resource_booking_api.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.raviraju.resource_booking_api.dto.ResourceRequest;
import com.raviraju.resource_booking_api.dto.ResourceResponse;
import com.raviraju.resource_booking_api.entity.Resource;
import com.raviraju.resource_booking_api.entity.ResourceType;
import com.raviraju.resource_booking_api.exception.ResourceNotFoundException;
import com.raviraju.resource_booking_api.repository.ResourceRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ResourceService {

    private final ResourceRepository resourceRepository;

    @Transactional
    public ResourceResponse createResource(ResourceRequest request) {
        Resource resource = Resource.builder()
                .name(request.getName())
                .type(request.getType())
                .description(request.getDescription())
                .available(request.getAvailable() != null ? request.getAvailable() : true)
                .build();

        Resource savedResource = resourceRepository.save(resource);
        return ResourceResponse.fromEntity(savedResource);
    }

    @Transactional(readOnly = true)
    public ResourceResponse getResourceById(Long id) {
        Resource resource = findResourceEntityById(id);
        return ResourceResponse.fromEntity(resource);
    }

    @Transactional(readOnly = true)
    public Resource findResourceEntityById(Long id) {
        return resourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found with id: " + id));
    }

    @Transactional(readOnly = true)
    public List<ResourceResponse> getAllResources(ResourceType type, Boolean available) {
        List<Resource> resources;

        if (type != null && available != null) {
            resources = resourceRepository.findByTypeAndAvailable(type, available);
        } else if (type != null) {
            resources = resourceRepository.findByType(type);
        } else if (available != null) {
            resources = resourceRepository.findByAvailable(available);
        } else {
            resources = resourceRepository.findAll();
        }

        return resources.stream()
                .map(ResourceResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public ResourceResponse updateResource(Long id, ResourceRequest request) {
        Resource resource = findResourceEntityById(id);

        resource.setName(request.getName());
        resource.setType(request.getType());
        resource.setDescription(request.getDescription());
        if (request.getAvailable() != null) {
            resource.setAvailable(request.getAvailable());
        }

        Resource updated = resourceRepository.save(resource);
        return ResourceResponse.fromEntity(updated);
    }

    @Transactional
    public void deleteResource(Long id) {
        if (!resourceRepository.existsById(id)) {
            throw new ResourceNotFoundException("Resource not found with id: " + id);
        }
        resourceRepository.deleteById(id);
    }
}
