package com.ranked4.auth.auth_service.auth.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ranked4.auth.auth_service.auth.model.RefreshToken;
import com.ranked4.auth.auth_service.auth.model.User;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    void deleteByUser(User user);
}
