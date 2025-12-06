package com.ranked4.auth.auth_service.auth.controller;

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
        AuthResponse authResponse = authService.login(
            request.username(),
            request.password()
        );
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