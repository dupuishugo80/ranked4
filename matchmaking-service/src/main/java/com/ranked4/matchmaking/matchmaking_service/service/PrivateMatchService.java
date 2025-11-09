package com.ranked4.matchmaking.matchmaking_service.service;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.ranked4.matchmaking.matchmaking_service.dto.MatchFoundEvent;
import com.ranked4.matchmaking.matchmaking_service.util.KafkaProducerConfig;

@Service
public class PrivateMatchService {

    private static final Logger log = LoggerFactory.getLogger(PrivateMatchService.class);

    private static final String LOBBY_KEY_PREFIX = "private_match_lobby:";
    private static final String USER_LOBBY_KEY_PREFIX = "user_private_lobby:";
    private static final Duration LOBBY_TTL = Duration.ofMinutes(30);

    private final RedisTemplate<String, Object> redisTemplate;
    private final KafkaTemplate<String, MatchFoundEvent> kafkaTemplate;
    private final Random random = new Random();

    public PrivateMatchService(RedisTemplate<String, Object> redisTemplate,
                               KafkaTemplate<String, MatchFoundEvent> kafkaTemplate) {
        this.redisTemplate = redisTemplate;
        this.kafkaTemplate = kafkaTemplate;
    }

    public Map<String, Object> createPrivateLobby(UUID hostUserId) {
        String existingCode = (String) redisTemplate.opsForValue()
                .get(USER_LOBBY_KEY_PREFIX + hostUserId);
        if (existingCode != null) {
            redisTemplate.delete(LOBBY_KEY_PREFIX + existingCode);
            redisTemplate.delete(USER_LOBBY_KEY_PREFIX + hostUserId);
        }

        String code = generateUniqueCode();
        String lobbyKey = LOBBY_KEY_PREFIX + code;

        Map<String, Object> lobby = new java.util.HashMap<>();
        lobby.put("code", code);
        lobby.put("hostUserId", hostUserId.toString());
        lobby.put("guestUserId", null);
        lobby.put("status", "LOBBY");
        lobby.put("createdAt", Instant.now().toEpochMilli());
        lobby.put("ranked", Boolean.FALSE);
        lobby.put("origin", "PRIVATE_1V1");
        lobby.put("matchId", null);

        redisTemplate.opsForValue().set(lobbyKey, lobby, LOBBY_TTL);
        redisTemplate.opsForValue().set(USER_LOBBY_KEY_PREFIX + hostUserId, code, LOBBY_TTL);

        log.info("Created private lobby {} for host {}", code, hostUserId);
        return lobby;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> joinPrivateLobby(UUID userId, String code) {
        String lobbyKey = LOBBY_KEY_PREFIX + code;
        Object raw = redisTemplate.opsForValue().get(lobbyKey);
        if (!(raw instanceof Map)) {
            throw new IllegalStateException("Lobby not found or expired");
        }
        Map<String, Object> lobby = (Map<String, Object>) raw;

        String status = Optional.ofNullable((String) lobby.get("status")).orElse("LOBBY");
        String hostUserId = (String) lobby.get("hostUserId");
        String guestUserId = (String) lobby.get("guestUserId");

        if (!"LOBBY".equals(status)) {
            throw new IllegalStateException("Lobby is not joinable");
        }
        if (hostUserId == null || hostUserId.equals(userId.toString())) {
            throw new IllegalStateException("Host cannot join their own lobby as guest");
        }
        if (guestUserId != null && !guestUserId.equals(userId.toString())) {
            throw new IllegalStateException("Lobby already has a guest");
        }

        lobby.put("guestUserId", userId.toString());

        redisTemplate.opsForValue().set(lobbyKey, lobby, LOBBY_TTL);
        log.info("User {} joined private lobby {}", userId, code);
        return lobby;
    }

    @SuppressWarnings("unchecked")
    public UUID startPrivateMatch(UUID hostUserId, String code) {
        String lobbyKey = LOBBY_KEY_PREFIX + code;
        Object raw = redisTemplate.opsForValue().get(lobbyKey);
        if (!(raw instanceof Map)) {
            throw new IllegalStateException("Lobby not found or expired");
        }
        Map<String, Object> lobby = (Map<String, Object>) raw;

        String status = Optional.ofNullable((String) lobby.get("status")).orElse("LOBBY");
        String storedHostId = (String) lobby.get("hostUserId");
        String guestUserId = (String) lobby.get("guestUserId");

        if (!hostUserId.toString().equals(storedHostId)) {
            throw new IllegalStateException("Only host can start the match");
        }
        if (!"LOBBY".equals(status)) {
            throw new IllegalStateException("Lobby already started or invalid");
        }
        if (guestUserId == null) {
            throw new IllegalStateException("Cannot start match without guest");
        }

        UUID matchId = UUID.randomUUID();

        MatchFoundEvent event = new MatchFoundEvent(
                UUID.fromString(storedHostId),
                UUID.fromString(guestUserId),
                false,
                "PRIVATE_1V1"
        );
        event.setMatchId(matchId);

        kafkaTemplate.send(KafkaProducerConfig.MATCH_FOUND_TOPIC, event.getMatchId().toString(), event);
        log.info("Private match started: matchId={}, host={}, guest={}", matchId, storedHostId, guestUserId);

        lobby.put("status", "STARTED");
        lobby.put("matchId", matchId.toString());
        lobby.put("ranked", Boolean.FALSE);
        lobby.put("origin", "PRIVATE_1V1");

        redisTemplate.opsForValue().set(lobbyKey, lobby, LOBBY_TTL);

        return matchId;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getLobby(String code) {
        String key = LOBBY_KEY_PREFIX + code;
        Object raw = redisTemplate.opsForValue().get(key);
        if (raw instanceof Map) {
            return (Map<String, Object>) raw;
        }
        return null;
    }

    private String generateUniqueCode() {
        String code;
        do {
            code = randomCode();
        } while (redisTemplate.opsForValue().get(LOBBY_KEY_PREFIX + code) != null);
        return code;
    }

    private String randomCode() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder sb = new StringBuilder(8);
        for (int i = 0; i < 8; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString().toUpperCase(Locale.ROOT);
    }
}