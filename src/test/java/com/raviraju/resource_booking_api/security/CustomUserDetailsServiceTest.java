package com.raviraju.resource_booking_api.security;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.raviraju.resource_booking_api.entity.Role;
import com.raviraju.resource_booking_api.entity.User;
import com.raviraju.resource_booking_api.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService userDetailsService;

    @Test
    void loadUserByUsername_Success() {
        User user = User.builder()
                .id(1L)
                .username("john")
                .password("encodedPassword")
                .role(Role.ADMIN)
                .build();

        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));

        UserDetails userDetails = userDetailsService.loadUserByUsername("john");

        assertNotNull(userDetails);
        assertEquals("john", userDetails.getUsername());
        assertTrue(userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
    }

    @Test
    void loadUserByUsername_NotFound_ThrowsException() {
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> userDetailsService.loadUserByUsername("unknown"));
    }
}
