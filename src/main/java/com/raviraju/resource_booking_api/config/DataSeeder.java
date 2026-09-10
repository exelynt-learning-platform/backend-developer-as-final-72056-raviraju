package com.raviraju.resource_booking_api.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.raviraju.resource_booking_api.entity.Resource;
import com.raviraju.resource_booking_api.entity.ResourceType;
import com.raviraju.resource_booking_api.entity.Role;
import com.raviraju.resource_booking_api.entity.User;
import com.raviraju.resource_booking_api.repository.ResourceRepository;
import com.raviraju.resource_booking_api.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Profile("dev")
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ResourceRepository resourceRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${SEED_ADMIN_PASSWORD:}")
    private String seedAdminPassword;

    @Value("${SEED_USER_PASSWORD:}")
    private String seedUserPassword;

    @Override
    public void run(String... args) {
        seedUsers();
        seedResources();
    }

    private void seedUsers() {
        if (!userRepository.existsByUsername("admin")) {
            String adminPassword = (seedAdminPassword != null && !seedAdminPassword.isBlank())
                    ? seedAdminPassword
                    : java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 16);
            User admin = User.builder()
                    .username("admin")
                    .email("admin@booking.com")
                    .password(passwordEncoder.encode(adminPassword))
                    .role(Role.ADMIN)
                    .build();
            userRepository.save(admin);
            log.info("Seeded dev ADMIN user (username='admin'). Configure SEED_ADMIN_PASSWORD to customize credentials.");
        }

        if (!userRepository.existsByUsername("user")) {
            String userPassword = (seedUserPassword != null && !seedUserPassword.isBlank())
                    ? seedUserPassword
                    : java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 16);
            User normalUser = User.builder()
                    .username("user")
                    .email("user@booking.com")
                    .password(passwordEncoder.encode(userPassword))
                    .role(Role.USER)
                    .build();
            userRepository.save(normalUser);
            log.info("Seeded dev USER user (username='user'). Configure SEED_USER_PASSWORD to customize credentials.");
        }
    }

    private void seedResources() {
        if (resourceRepository.count() == 0) {
            List<Resource> defaultResources = List.of(
                    Resource.builder()
                            .name("Executive Conference Room A")
                            .type(ResourceType.ROOM)
                            .description("Spacious meeting room with 4K display, video conferencing, and 14-person seating.")
                            .available(true)
                            .build(),
                    Resource.builder()
                            .name("Fleet Transit Van #01")
                            .type(ResourceType.VEHICLE)
                            .description("Commercial cargo van with GPS tracking for equipment transport.")
                            .available(true)
                            .build(),
                    Resource.builder()
                            .name("Sony 4K HDR Laser Projector")
                            .type(ResourceType.EQUIPMENT)
                            .description("Ultra high-definition portable projector with wireless casting capabilities.")
                            .available(true)
                            .build(),
                    Resource.builder()
                            .name("Hardware Engineering Lab 3")
                            .type(ResourceType.ROOM)
                            .description("Testing lab with oscilloscopes, soldering stations, and 3D printers.")
                            .available(false)
                            .build()
            );

            resourceRepository.saveAll(defaultResources);
            log.info("Seeded {} default bookable resources.", defaultResources.size());
        }
    }
}
