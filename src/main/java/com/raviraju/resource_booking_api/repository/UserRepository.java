package com.raviraju.resource_booking_api.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.raviraju.resource_booking_api.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);
}
