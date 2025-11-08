package com.ranked4.matchmaking.matchmaking_service.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.ranked4.matchmaking.matchmaking_service.dto.PlayerDisconnectEvent;
import com.ranked4.matchmaking.matchmaking_service.service.MatchmakingService;

@Service
public class MatchmakingEventListener {

    private static final Logger log = LoggerFactory.getLogger(MatchmakingEventListener.class);
    private final MatchmakingService matchmakingService;

    public MatchmakingEventListener(MatchmakingService matchmakingService) {
        this.matchmakingService = matchmakingService;
    }

    @KafkaListener(
        topics = "player.disconnected",
        groupId = "${spring.kafka.consumer.group-id}",
        containerFactory = "playerDisconnectListenerContainerFactory"
    )
    public void handlePlayerDisconnect(PlayerDisconnectEvent event) {
        log.warn("PlayerDisconnectEvent received for {}. Cleaning up the queue.", event.getPlayerId());
        try {
            matchmakingService.leaveQueue(event.getPlayerId());
        } catch (Exception e) {
            log.error("Error while cleaning up the queue for {}: {}", event.getPlayerId(), e.getMessage());
        }
    }
}