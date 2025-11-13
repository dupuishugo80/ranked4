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
public class KafkaService {

    private static final Logger log = LoggerFactory.getLogger(KafkaService.class);
    private final GameService gameService;
    private final SimpMessagingTemplate messagingTemplate;
    private final GameSessionRegistry gameSessionRegistry;
    private final java.util.concurrent.ScheduledExecutorService scheduler = java.util.concurrent.Executors.newSingleThreadScheduledExecutor();

    public KafkaService(GameService gameService, SimpMessagingTemplate messagingTemplate, GameSessionRegistry gameSessionRegistry) {
        this.gameService = gameService;
        this.messagingTemplate = messagingTemplate;
        this.gameSessionRegistry = gameSessionRegistry;
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
                event.getPlayerTwoId(),
                event.isRanked(),
                event.getOrigin()
            );

            log.info("Send initial game state for match {} to players", event.getMatchId());

            GameUpdateDTO gameUpdate = gameService.createGameUpdateDTO(newGame);
            String lobbyDestination = "/topic/lobby";
            messagingTemplate.convertAndSend(lobbyDestination, gameUpdate);

            String destination = "/topic/game/" + event.getMatchId();
            messagingTemplate.convertAndSend(destination, gameUpdate);
            log.info("Initial game state for match {} sent to {}", event.getMatchId(), destination);

            if (event.isRanked()) {
                scheduleNoShowCheck(event.getMatchId(), event.getPlayerOneId(), event.getPlayerTwoId());
            }
        } catch (Exception e) {
            log.error("Error processing MatchFoundEvent event: {}", event, e);
        }
    }

    private void scheduleNoShowCheck(java.util.UUID gameId, java.util.UUID playerOneId, java.util.UUID playerTwoId) {
        scheduler.schedule(() -> {
            try {
                boolean p1Connected = gameSessionRegistry.isPlayerConnectedToGame(playerOneId, gameId);
                boolean p2Connected = gameSessionRegistry.isPlayerConnectedToGame(playerTwoId, gameId);

                if (p1Connected && p2Connected) {
                    return;
                }

                Game updated = gameService.cancelGameNoShow(gameId);

                if (updated.isRanked()) {
                    return;
                }

                if (updated.getOrigin() != null && updated.getOrigin().equals("CANCELLED_NO_SHOW")) {
                    GameUpdateDTO updateDTO = gameService.createGameUpdateDTO(updated);
                    messagingTemplate.convertAndSend("/topic/game/" + gameId, updateDTO);
                    log.warn("Ranked game {} cancelled due to no-show (p1Connected={}, p2Connected={}).", gameId, p1Connected, p2Connected);
                }
            } catch (Exception e) {
                log.error("Error during no-show check for game {}", gameId, e);
            }
        }, 10, java.util.concurrent.TimeUnit.SECONDS);
    }
}