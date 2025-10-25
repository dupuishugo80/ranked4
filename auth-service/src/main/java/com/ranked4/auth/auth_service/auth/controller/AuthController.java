package com.ranked4.auth.auth_service.auth.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ranked4.auth.auth_service.auth.dto.AuthResponse;
import com.ranked4.auth.auth_service.auth.dto.LoginRequest;
import com.ranked4.auth.auth_service.auth.dto.LogoutResponse;
import com.ranked4.auth.auth_service.auth.dto.ProfileResponse;
import com.ranked4.auth.auth_service.auth.dto.RefreshTokenRequest;
import com.ranked4.auth.auth_service.auth.dto.RegisterRequest;
import com.ranked4.auth.auth_service.auth.dto.RegisterResponse;
import com.ranked4.auth.auth_service.auth.model.User;
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
            request.getUsername(),
            request.getEmail(),
            request.getPassword()
        );
        return ResponseEntity.ok(new RegisterResponse("User registered successfully"));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        AuthResponse authResponse = authService.login(
            request.getUsername(),
            request.getPassword()
        );
        return ResponseEntity.ok(authResponse);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(@RequestBody RefreshTokenRequest request) {
        AuthResponse authResponse = authService.refreshAccessToken(request.getRefreshToken());
        return ResponseEntity.ok(authResponse);
    }

    @PostMapping("/logout")
    public ResponseEntity<LogoutResponse> logout(@RequestBody RefreshTokenRequest request) {
        authService.logout(request.getRefreshToken());
        return ResponseEntity.ok(new LogoutResponse("Logged out successfully"));
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> getProfile(@AuthenticationPrincipal User user) {
        try {
            ProfileResponse response = new ProfileResponse(
                user.getUsername(),
                user.getEmail(),
                String.join(",", user.getRoles())
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("An error occurred while fetching the profile");
        }
    }
}