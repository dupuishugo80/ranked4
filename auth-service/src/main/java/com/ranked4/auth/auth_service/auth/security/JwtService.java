package com.ranked4.auth.auth_service.auth.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.ranked4.auth.auth_service.auth.model.User;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class JwtService {

    private final SecretKey secretKey;
    private final long jwtAccessTokenExpirationMs;
    private final long jwtRefreshTokenExpirationMs;

    public JwtService(
            @Value("${jwt.secret}") String jwtSecret,
            @Value("${jwt.access-token-expiration}") long jwtAccessTokenExpirationMs,
            @Value("${jwt.refresh-token-expiration}") long jwtRefreshTokenExpirationMs) {
        
        this.secretKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        this.jwtAccessTokenExpirationMs = jwtAccessTokenExpirationMs;
        this.jwtRefreshTokenExpirationMs = jwtRefreshTokenExpirationMs;
    }

    public String generateToken(User user) {
        return generateTokenWithExpiration(user, jwtAccessTokenExpirationMs);
    }

    public String generateRefreshToken(User user) {
        return generateTokenWithExpiration(user, jwtRefreshTokenExpirationMs);
    }

    private String generateTokenWithExpiration(User user, long expirationMs) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(user.getUsername())
                .claim("userId", user.getId())
                .claim("roles", user.getRoles())
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(secretKey)
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parse(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String getUsernameFromToken(String token) {
        return extractAllClaims(token).getSubject();
    }

    public List<String> getRolesFromToken(String token) {
        Claims claims = extractAllClaims(token);
        List<?> roles = claims.get("roles", List.class);
        if (roles == null) {
            return List.of();
        }
        return roles.stream()
                .map(String::valueOf)
                .collect(Collectors.toList());
    }

    public Long getUserIdFromToken(String token) {
        return extractAllClaims(token).get("userId", Long.class); 
    }

    public Date getExpirationDateFromToken(String token) {
        return extractAllClaims(token).getExpiration();
    }

    public boolean isTokenExpired(String token) {
        try {
            return extractAllClaims(token).getExpiration().before(new Date());
        } catch (Exception e) {
            return true;
        }
    }
}
