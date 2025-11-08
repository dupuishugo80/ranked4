package com.ranked4.matchmaking.matchmaking_service.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.ranked4.matchmaking.matchmaking_service.dto.MatchFoundEvent;
import com.ranked4.matchmaking.matchmaking_service.dto.UserProfileDTO;
import com.ranked4.matchmaking.matchmaking_service.util.KafkaProducerConfig;

import reactor.core.publisher.Mono;

@Service
public class MatchmakingService {

    private static final Logger log = LoggerFactory.getLogger(MatchmakingService.class);
    private static final String MATCHMAKING_QUEUE_KEY = "matchmaking_queue_ranked4";
    private static final int ELO_RANGE = 200;
    private static final long STALE_ENTRY_TIMEOUT_MINUTES = 5;

    private final WebClient userProfileWebClient;
    private final RedisTemplate<String, String> matchmakingRedisTemplate;
    private final ZSetOperations<String, String> zSetOps;
    private final KafkaTemplate<String, MatchFoundEvent> kafkaTemplate;
    private static final String MATCHMAKING_TIMESTAMPS_KEY = "matchmaking_timestamps";
    private final HashOperations<String, String, Long> hashOps;

    public MatchmakingService(
            @Qualifier("userProfileWebClient") WebClient userProfileWebClient,
            @Qualifier("matchmakingRedisTemplate") RedisTemplate<String, String> matchmakingRedisTemplate,
            @Qualifier("redisTemplate") RedisTemplate<String, Object> redisTemplate,
            KafkaTemplate<String, MatchFoundEvent> kafkaTemplate) {
        this.userProfileWebClient = userProfileWebClient;
        this.matchmakingRedisTemplate = matchmakingRedisTemplate;
        this.zSetOps = matchmakingRedisTemplate.opsForZSet();
        this.kafkaTemplate = kafkaTemplate;
        this.hashOps = redisTemplate.opsForHash();
    }

    public void joinQueue(UUID userId) {
        fetchUserProfile(userId)
            .flatMap(profile -> {
                log.info("Recovered profile for {}: ELO={}", profile.getDisplayName(), profile.getElo());

                return findAndProcessMatch(profile)
                    .switchIfEmpty(Mono.defer(() -> { 
                        log.info("No match found for {}. Adding to queue.", userId);
                        return addUserToQueue(profile)
                            .thenReturn(false);
                    }));
            })
            .subscribe(
                matchFound -> {
                    if (Boolean.TRUE.equals(matchFound)) {
                        log.info("Matchmaking successful for {}", userId);
                    } else {
                        log.info("Player {} added to queue.", userId);
                    }
                },
                error -> log.error("Error during matchmaking process for {}: {}", userId, error.getMessage())
            );
    }
    
    public void leaveQueue(UUID userId) {
        log.info("Player {} left the queue.", userId);
        zSetOps.remove(MATCHMAKING_QUEUE_KEY, userId.toString());
    }

    private Mono<UserProfileDTO> fetchUserProfile(UUID userId) {
        return userProfileWebClient.get()
                .uri("/api/profiles/" + userId.toString())
                .retrieve()
                .bodyToMono(UserProfileDTO.class)
                .doOnError(e -> log.error("Unable to retrieve profile for user {}", userId, e));
    }

    private Mono<Boolean> findAndProcessMatch(UserProfileDTO searchingPlayer) {
        double minElo = searchingPlayer.getElo() - ELO_RANGE;
        double maxElo = searchingPlayer.getElo() + ELO_RANGE;
        UUID searchingPlayerId = searchingPlayer.getUserId();

        Set<String> compatiblePlayers = zSetOps.rangeByScore(MATCHMAKING_QUEUE_KEY, minElo, maxElo);

        if (compatiblePlayers == null || compatiblePlayers.isEmpty()) {
            return Mono.empty();
        }

        String opponentIdStr = compatiblePlayers.iterator().next();
        UUID opponentId = UUID.fromString(opponentIdStr);

        if (searchingPlayerId.equals(opponentId)) {
            log.warn("Player {} attempted to match with themselves. Ignored.", searchingPlayerId);
            return Mono.empty();
        }

        zSetOps.remove(MATCHMAKING_QUEUE_KEY, searchingPlayerId.toString());
        hashOps.delete(MATCHMAKING_TIMESTAMPS_KEY, searchingPlayerId.toString());
        zSetOps.remove(MATCHMAKING_QUEUE_KEY, opponentIdStr);
        hashOps.delete(MATCHMAKING_TIMESTAMPS_KEY, opponentIdStr);

        log.info("Match found! {} vs {}", searchingPlayerId, opponentId);

        MatchFoundEvent event = new MatchFoundEvent(searchingPlayerId, opponentId);

        return Mono.fromFuture(kafkaTemplate.send(KafkaProducerConfig.MATCH_FOUND_TOPIC, event.getMatchId().toString(), event))
            .doOnSuccess(result -> log.info("MatchFoundEvent event sent to Kafka: matchId {}", event.getMatchId()))
            .thenReturn(true);
    }

    private Mono<Void> addUserToQueue(UserProfileDTO profile) {
        log.info("Adding {} to queue (ELO: {})", profile.getUserId(), profile.getElo());
        String userIdStr = profile.getUserId().toString();
        zSetOps.add(MATCHMAKING_QUEUE_KEY, userIdStr, profile.getElo());
        hashOps.put(MATCHMAKING_TIMESTAMPS_KEY, userIdStr, Instant.now().toEpochMilli());
        return Mono.empty();
    }

    @Scheduled(fixedRate = 60000) 
    public void cleanupStaleEntries() {
        log.info("[Scheduler] Cleaning expired entries from the queue...");

        long fiveMinutesAgo = Instant.now()
                                .minus(STALE_ENTRY_TIMEOUT_MINUTES, ChronoUnit.MINUTES)
                                .toEpochMilli();
        
        Map<String, Long> allTimestamps = hashOps.entries(MATCHMAKING_TIMESTAMPS_KEY);
        if (allTimestamps.isEmpty()) {
            log.info("[Scheduler] No entries in the queue, cleanup complete.");
            return;
        }

        for (Map.Entry<String, Long> entry : allTimestamps.entrySet()) {
            if (entry.getValue() < fiveMinutesAgo) {
                String staleUserId = entry.getKey();
                log.warn("[Scheduler] Player {} has been in the queue for over 5 minutes. Removing...", staleUserId);
                
                zSetOps.remove(MATCHMAKING_QUEUE_KEY, staleUserId);
                hashOps.delete(MATCHMAKING_TIMESTAMPS_KEY, staleUserId);
            }
        }
    }
}