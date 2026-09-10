package com.raviraju.resource_booking_api.service;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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

import com.raviraju.resource_booking_api.dto.ResourceRequest;
import com.raviraju.resource_booking_api.dto.ResourceResponse;
import com.raviraju.resource_booking_api.entity.Resource;
import com.raviraju.resource_booking_api.entity.ResourceType;
import com.raviraju.resource_booking_api.exception.ResourceNotFoundException;
import com.raviraju.resource_booking_api.repository.ResourceRepository;

@ExtendWith(MockitoExtension.class)
class ResourceServiceTest {

    @Mock
    private ResourceRepository resourceRepository;

    @InjectMocks
    private ResourceService resourceService;

    private Resource sampleResource;

    @BeforeEach
    void setUp() {
        sampleResource = Resource.builder()
                .id(1L)
                .name("Conference Room A")
                .type(ResourceType.ROOM)
                .description("Projector equipped")
                .available(true)
                .build();
    }

    @Test
    void createResource_Success() {
        ResourceRequest request = ResourceRequest.builder()
                .name("Conference Room A")
                .type(ResourceType.ROOM)
                .description("Projector equipped")
                .available(true)
                .build();

        when(resourceRepository.save(any(Resource.class))).thenReturn(sampleResource);

        ResourceResponse response = resourceService.createResource(request);

        assertNotNull(response);
        assertEquals("Conference Room A", response.getName());
        assertEquals(ResourceType.ROOM, response.getType());
        assertTrue(response.isAvailable());
        verify(resourceRepository).save(any(Resource.class));
    }

    @Test
    void createResource_DefaultAvailableTrueWhenNull() {
        ResourceRequest request = ResourceRequest.builder()
                .name("Laptop 1")
                .type(ResourceType.EQUIPMENT)
                .description("MacBook Pro")
                .available(null)
                .build();

        Resource saved = Resource.builder()
                .id(2L)
                .name("Laptop 1")
                .type(ResourceType.EQUIPMENT)
                .description("MacBook Pro")
                .available(true)
                .build();

        when(resourceRepository.save(any(Resource.class))).thenReturn(saved);

        ResourceResponse response = resourceService.createResource(request);

        assertNotNull(response);
        assertTrue(response.isAvailable());
    }

    @Test
    void getResourceById_Success() {
        when(resourceRepository.findById(1L)).thenReturn(Optional.of(sampleResource));

        ResourceResponse response = resourceService.getResourceById(1L);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("Conference Room A", response.getName());
    }

    @Test
    void getResourceById_NotFound_ThrowsException() {
        when(resourceRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> resourceService.getResourceById(99L));
    }

    @Test
    void getAllResources_AllFiltersNull() {
        when(resourceRepository.findAll()).thenReturn(List.of(sampleResource));

        List<ResourceResponse> list = resourceService.getAllResources(null, null);

        assertEquals(1, list.size());
        verify(resourceRepository).findAll();
    }

    @Test
    void getAllResources_FilterByTypeAndAvailable() {
        when(resourceRepository.findByTypeAndAvailable(ResourceType.ROOM, true)).thenReturn(List.of(sampleResource));

        List<ResourceResponse> list = resourceService.getAllResources(ResourceType.ROOM, true);

        assertEquals(1, list.size());
        verify(resourceRepository).findByTypeAndAvailable(ResourceType.ROOM, true);
    }

    @Test
    void getAllResources_FilterByTypeOnly() {
        when(resourceRepository.findByType(ResourceType.ROOM)).thenReturn(List.of(sampleResource));

        List<ResourceResponse> list = resourceService.getAllResources(ResourceType.ROOM, null);

        assertEquals(1, list.size());
        verify(resourceRepository).findByType(ResourceType.ROOM);
    }

    @Test
    void getAllResources_FilterByAvailableOnly() {
        when(resourceRepository.findByAvailable(true)).thenReturn(List.of(sampleResource));

        List<ResourceResponse> list = resourceService.getAllResources(null, true);

        assertEquals(1, list.size());
        verify(resourceRepository).findByAvailable(true);
    }

    @Test
    void updateResource_Success() {
        ResourceRequest updateRequest = ResourceRequest.builder()
                .name("Updated Room")
                .type(ResourceType.ROOM)
                .description("Updated desc")
                .available(false)
                .build();

        when(resourceRepository.findById(1L)).thenReturn(Optional.of(sampleResource));
        when(resourceRepository.save(any(Resource.class))).thenReturn(sampleResource);

        ResourceResponse response = resourceService.updateResource(1L, updateRequest);

        assertNotNull(response);
        verify(resourceRepository).save(sampleResource);
    }

    @Test
    void updateResource_NotFound_ThrowsException() {
        ResourceRequest updateRequest = ResourceRequest.builder()
                .name("Updated Room")
                .build();

        when(resourceRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> resourceService.updateResource(999L, updateRequest));
    }

    @Test
    void deleteResource_Success() {
        when(resourceRepository.existsById(1L)).thenReturn(true);

        resourceService.deleteResource(1L);

        verify(resourceRepository).deleteById(1L);
    }

    @Test
    void deleteResource_NotFound_ThrowsException() {
        when(resourceRepository.existsById(99L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> resourceService.deleteResource(99L));
        verify(resourceRepository, never()).deleteById(any());
    }
}
