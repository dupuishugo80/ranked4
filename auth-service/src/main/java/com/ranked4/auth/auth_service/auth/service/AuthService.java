package com.ranked4.auth.auth_service.auth.service;

import com.ranked4.auth.auth_service.auth.dto.AuthResponse;
import com.ranked4.auth.auth_service.auth.dto.UserRegisteredEvent;
import com.ranked4.auth.auth_service.auth.model.RefreshToken;
import com.ranked4.auth.auth_service.auth.model.User;
import com.ranked4.auth.auth_service.auth.repository.RefreshTokenRepository;
import com.ranked4.auth.auth_service.auth.repository.UserRepository;
import com.ranked4.auth.auth_service.auth.security.JwtService;
import com.ranked4.auth.auth_service.auth.util.KafkaProducerConfig;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService implements UserDetailsService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final long refreshTokenExpirationMs;
    private final KafkaTemplate<String, UserRegisteredEvent> kafkaTemplate;

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
                       @Value("${jwt.refresh-token-expiration}") long refreshTokenExpirationMs,
                       KafkaTemplate<String, UserRegisteredEvent> kafkaTemplate) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenRepository = refreshTokenRepository;
        this.refreshTokenExpirationMs = refreshTokenExpirationMs;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) {
        String cleanedUsername = username != null ? username.trim() : "";

        log.info("=== Login attempt for username: '{}' ===", cleanedUsername);
        log.debug("Original username length: {}, cleaned length: {}", username != null ? username.length() : 0, cleanedUsername.length());
        log.debug("Original username bytes: {}", username != null ? java.util.Arrays.toString(username.getBytes(StandardCharsets.UTF_8)) : "null");
        log.debug("Cleaned username bytes: {}", java.util.Arrays.toString(cleanedUsername.getBytes(StandardCharsets.UTF_8)));

        return userRepository.findByUsername(cleanedUsername)
                .orElseThrow(() -> {
                    log.error("User not found in database: '{}' (length: {})", cleanedUsername, cleanedUsername.length());
                    return new BadCredentialsException("User not found : " + cleanedUsername);
                });
    }

    @Transactional(readOnly = true)
    public User loadUserEntityByUsername(String username) {
        String cleanedUsername = username != null ? username.trim() : "";

        log.info("=== Loading user entity for username: '{}' ===", cleanedUsername);
        log.debug("Original username length: {}, cleaned length: {}", username != null ? username.length() : 0, cleanedUsername.length());

        return userRepository.findByUsername(cleanedUsername)
                .orElseThrow(() -> {
                    log.error("User entity not found in database: '{}' (length: {})", cleanedUsername, cleanedUsername.length());
                    return new BadCredentialsException("User not found : " + cleanedUsername);
                });
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

        User savedUser = userRepository.save(user);

         try {
            UserRegisteredEvent event = new UserRegisteredEvent(
                    savedUser.getId(),
                    savedUser.getUsername(),
                    savedUser.getEmail()
            );
            kafkaTemplate.send(KafkaProducerConfig.TOPIC_USER_REGISTERED, event);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send UserRegisteredEvent to Kafka", e);
        }
    }

    @Transactional
    public AuthResponse login(String username, String password) {
        log.info("Login request received - Username: '{}', password provided: {}", username, password != null && !password.isEmpty());

        User user = (User) loadUserByUsername(username);

        log.info("User found in database: ID={}, Username='{}', Email='{}'", user.getId(), user.getUsername(), user.getEmail());

        if (!passwordEncoder.matches(password, user.getPassword())) {
            log.warn("Password mismatch for user: '{}'", user.getUsername());
            throw new BadCredentialsException("Incorrect password");
        }

        log.info("Password validated successfully for user: '{}'", user.getUsername());

        String accessToken = jwtService.generateToken(user);
        RefreshToken refreshToken = createAndSaveRefreshToken(user);

        log.info("Login successful for user: '{}'", user.getUsername());

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