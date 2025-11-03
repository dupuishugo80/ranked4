package com.ranked4.game.game_service.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.ranked4.game.game_service.dto.GameUpdateDTO;
import com.ranked4.game.game_service.dto.MatchFoundEvent;
import com.ranked4.game.game_service.model.Game;
import com.ranked4.game.game_service.service.GameService;

@Service
public class KafkaConsumerService {

    private static final Logger log = LoggerFactory.getLogger(KafkaConsumerService.class);
    private final GameService gameService;
    private final SimpMessagingTemplate messagingTemplate;

    public KafkaConsumerService(GameService gameService, SimpMessagingTemplate messagingTemplate) {
        this.gameService = gameService;
        this.messagingTemplate = messagingTemplate;
    }

    @KafkaListener(
        topics = "match.found", 
        groupId = "game-service-group",
        containerFactory = "matchFoundListenerContainerFactory"
    )
    public void handleMatchFound(MatchFoundEvent event) {
        try {
            log.info("MatchFoundEvent event received: {}", event);

            Game newGame = gameService.createGame(
                event.getMatchId(),
                event.getPlayerOneId(),
                event.getPlayerTwoId()
            );

            log.info("Send initial game state for match {} to players", event.getMatchId());

            GameUpdateDTO gameUpdate = GameUpdateDTO.fromEntity(newGame);
            String lobbyDestination = "/topic/lobby";
            messagingTemplate.convertAndSend(lobbyDestination, gameUpdate);

            String destination = "/topic/game/" + event.getMatchId();
            messagingTemplate.convertAndSend(destination, gameUpdate);
            log.info("Initial game state for match {} sent to {}", event.getMatchId(), destination);
        } catch (Exception e) {
            log.error("Error processing MatchFoundEvent event: {}", event, e);
        }
    }
}