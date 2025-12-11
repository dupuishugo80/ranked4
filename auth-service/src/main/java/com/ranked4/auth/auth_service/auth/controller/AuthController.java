package com.ranked4.auth.auth_service.auth.controller;

import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ranked4.auth.auth_service.auth.dto.AuthResponse;
import com.ranked4.auth.auth_service.auth.dto.LoginRequest;
import com.ranked4.auth.auth_service.auth.dto.LogoutResponse;
import com.ranked4.auth.auth_service.auth.dto.RefreshTokenRequest;
import com.ranked4.auth.auth_service.auth.dto.RegisterRequest;
import com.ranked4.auth.auth_service.auth.dto.RegisterResponse;
import com.ranked4.auth.auth_service.auth.service.AuthService;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@RequestBody RegisterRequest request) {
        authService.register(
            request.username(),
            request.email(),
            request.password()
        );
        return ResponseEntity.ok(new RegisterResponse("User registered successfully"));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        log.info("=== HTTP POST /api/auth/login received ===");
        log.info("Request username: '{}'", request.username());
        log.debug("Username length: {}", request.username() != null ? request.username().length() : 0);
        log.debug("Username bytes: {}", request.username() != null ? java.util.Arrays.toString(request.username().getBytes(StandardCharsets.UTF_8)) : "null");
        log.debug("Username contains whitespace at start: {}, at end: {}",
            request.username() != null && request.username().startsWith(" "),
            request.username() != null && request.username().endsWith(" "));

        AuthResponse authResponse = authService.login(
            request.username(),
            request.password()
        );

        log.info("Login successful via HTTP for username: '{}'", request.username());
        return ResponseEntity.ok(authResponse);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(@RequestBody RefreshTokenRequest request) {
        AuthResponse authResponse = authService.refreshAccessToken(request.refreshToken());
        return ResponseEntity.ok(authResponse);
    }

    @PostMapping("/logout")
    public ResponseEntity<LogoutResponse> logout(@RequestBody RefreshTokenRequest request) {
        authService.logout(request.refreshToken());
        return ResponseEntity.ok(new LogoutResponse("Logged out successfully"));
    }
}