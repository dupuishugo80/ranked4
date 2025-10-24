package com.ranked4.auth.auth_service.auth.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ranked4.auth.auth_service.auth.model.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    Optional<User> findByRefreshToken(String refreshToken);

    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
}