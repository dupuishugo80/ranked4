package com.ranked4.auth.auth_service.auth.service;

import com.ranked4.auth.auth_service.auth.dto.AuthResponse;
import com.ranked4.auth.auth_service.auth.dto.LoginRequest;
import com.ranked4.auth.auth_service.auth.dto.RegisterRequest;
import com.ranked4.auth.auth_service.auth.model.RefreshToken;
import com.ranked4.auth.auth_service.auth.model.User;
import com.ranked4.auth.auth_service.auth.repository.RefreshTokenRepository;
import com.ranked4.auth.auth_service.auth.repository.UserRepository;
import com.ranked4.auth.auth_service.auth.security.JwtService;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final long refreshTokenExpirationMs;

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
    );
    private static final int MIN_PASSWORD_LENGTH = 8;
    private static final Pattern UPPERCASE_PATTERN = Pattern.compile(".*[A-Z].*");
    private static final Pattern LOWERCASE_PATTERN = Pattern.compile(".*[a-z].*");
    private static final Pattern DIGIT_PATTERN = Pattern.compile(".*[0-9].*");
    private static final Pattern SPECIAL_CHAR_PATTERN = Pattern.compile(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*");


    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       RefreshTokenRepository refreshTokenRepository,
                       @Value("${jwt.refresh-token-expiration}") long refreshTokenExpirationMs) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenRepository = refreshTokenRepository;
        this.refreshTokenExpirationMs = refreshTokenExpirationMs;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new BadCredentialsException("User not found : " + username));
    }

    @Transactional(readOnly = true)
    public User loadUserEntityByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new BadCredentialsException("User not found : " + username));
    }

    @Transactional
    public void register(String username, String email, String password) {
        validateEmail(email);
        validatePassword(password);

        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Error: This username is already taken!");
        }
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Error: This email is already in use!");
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setRoles(List.of("ROLE_USER"));

        userRepository.save(user);
    }

    @Transactional
    public AuthResponse login(String username, String password) {
        User user = (User) loadUserByUsername(username);

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BadCredentialsException("Incorrect password");
        }

        String accessToken = jwtService.generateToken(user);
        RefreshToken refreshToken = createAndSaveRefreshToken(user);

        return new AuthResponse(accessToken, refreshToken.getToken());
    }

    @Transactional
    public void logout(String token) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new BadCredentialsException("Refresh token not found."));
        refreshTokenRepository.delete(refreshToken);
    }

    @Transactional
    public AuthResponse refreshAccessToken(String token) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new BadCredentialsException("Refresh token not found."));

        if (refreshToken.getExpiryDate().isBefore(Instant.now())) {
            refreshTokenRepository.delete(refreshToken);
            throw new BadCredentialsException("Refresh token expired. Please log in again.");
        }

        User user = refreshToken.getUser();
        String newAccessToken = jwtService.generateToken(user);

        return new AuthResponse(newAccessToken, token);
    }

    private RefreshToken createAndSaveRefreshToken(User user) {
        refreshTokenRepository.deleteByUser(user);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setExpiryDate(Instant.now().plusMillis(refreshTokenExpirationMs));
        refreshToken.setToken(UUID.randomUUID().toString());

        return refreshTokenRepository.save(refreshToken);
    }

    private void validateEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email is required");
        }
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new IllegalArgumentException("Invalid email format");
        }
    }

    private void validatePassword(String password) {
        if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalArgumentException("Password must be at least " + MIN_PASSWORD_LENGTH + " characters long");
        }
        if (!UPPERCASE_PATTERN.matcher(password).matches()) {
            throw new IllegalArgumentException("Password must contain at least one uppercase letter");
        }
        if (!LOWERCASE_PATTERN.matcher(password).matches()) {
            throw new IllegalArgumentException("Password must contain at least one lowercase letter");
        }
        if (!DIGIT_PATTERN.matcher(password).matches()) {
            throw new IllegalArgumentException("Password must contain at least one digit");
        }
        if (!SPECIAL_CHAR_PATTERN.matcher(password).matches()) {
            throw new IllegalArgumentException("Password must contain at least one special character");
        }
    }
}