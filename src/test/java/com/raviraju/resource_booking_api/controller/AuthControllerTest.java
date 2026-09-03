package com.raviraju.resource_booking_api.controller;

import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.raviraju.resource_booking_api.dto.LoginRequest;
import com.raviraju.resource_booking_api.dto.LoginResponse;
import com.raviraju.resource_booking_api.dto.RegisterRequest;
import com.raviraju.resource_booking_api.entity.Role;
import com.raviraju.resource_booking_api.service.AuthService;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @MockitoBean
    private AuthService authService;

    @Test
    void register_Success() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .username("newuser")
                .password("password123")
                .email("new@booking.com")
                .role(Role.USER)
                .build();

        LoginResponse response = new LoginResponse("mockJwtToken", "newuser", "USER");
        when(authService.register(any(RegisterRequest.class))).thenReturn(response);

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("mockJwtToken"))
                .andExpect(jsonPath("$.username").value("newuser"));
    }

    @Test
    void login_Success() throws Exception {
        LoginRequest request = new LoginRequest("testuser", "password123");
        LoginResponse response = new LoginResponse("mockJwtToken", "testuser", "USER");

        when(authService.login(any(LoginRequest.class))).thenReturn(response);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("mockJwtToken"));
    }
}
