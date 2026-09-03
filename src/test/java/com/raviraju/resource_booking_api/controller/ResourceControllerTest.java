package com.raviraju.resource_booking_api.controller;

import java.util.List;

import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.raviraju.resource_booking_api.dto.ResourceRequest;
import com.raviraju.resource_booking_api.dto.ResourceResponse;
import com.raviraju.resource_booking_api.entity.ResourceType;
import com.raviraju.resource_booking_api.service.ResourceService;

@SpringBootTest
@AutoConfigureMockMvc
class ResourceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @MockitoBean
    private ResourceService resourceService;

    @Test
    @WithMockUser(roles = "USER")
    void getAllResources_Authenticated_Success() throws Exception {
        ResourceResponse res = ResourceResponse.builder()
                .id(1L)
                .name("Conference Room")
                .type(ResourceType.ROOM)
                .available(true)
                .build();

        when(resourceService.getAllResources(null, null)).thenReturn(List.of(res));

        mockMvc.perform(get("/api/resources"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Conference Room"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void createResource_AsUser_Forbidden() throws Exception {
        ResourceRequest request = ResourceRequest.builder()
                .name("New Van")
                .type(ResourceType.VEHICLE)
                .available(true)
                .build();

        mockMvc.perform(post("/api/resources")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createResource_AsAdmin_Success() throws Exception {
        ResourceRequest request = ResourceRequest.builder()
                .name("New Van")
                .type(ResourceType.VEHICLE)
                .available(true)
                .build();

        ResourceResponse response = ResourceResponse.builder()
                .id(2L)
                .name("New Van")
                .type(ResourceType.VEHICLE)
                .available(true)
                .build();

        when(resourceService.createResource(any(ResourceRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/resources")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(2L))
                .andExpect(jsonPath("$.name").value("New Van"));
    }
}
