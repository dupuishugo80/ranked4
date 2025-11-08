package com.ranked4.game.game_service.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import com.ranked4.game.game_service.dto.GameUpdateDTO;
import com.ranked4.game.game_service.dto.PlayerDisconnectEvent;
import com.ranked4.game.game_service.model.Game;
import com.ranked4.game.game_service.service.GameService;

@Component
public class WebSocketEventListener implements ApplicationListener<SessionDisconnectEvent> {

    private static final Logger log = LoggerFactory.getLogger(WebSocketEventListener.class);

    private final GameSessionRegistry sessionRegistry;
    private final GameService gameService;
    private final SimpMessageSendingOperations messagingTemplate;
    private final KafkaTemplate<String, PlayerDisconnectEvent> kafkaTemplate;

    public WebSocketEventListener(GameSessionRegistry sessionRegistry,
                                  GameService gameService,
                                  SimpMessageSendingOperations messagingTemplate,
                                  KafkaTemplate<String, PlayerDisconnectEvent> kafkaTemplate) {
        this.sessionRegistry = sessionRegistry;
        this.gameService = gameService;
        this.messagingTemplate = messagingTemplate;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public void onApplicationEvent(SessionDisconnectEvent event) {
        String sessionId = event.getSessionId();
        log.info("WebSocket disconnection detected: {}", sessionId);

        GameSessionRegistry.PlayerSessionInfo sessionInfo = sessionRegistry.unregisterSession(sessionId);

        if (sessionInfo != null) {
            if (sessionInfo.isInGame()) {
                log.warn("Player {} disconnected from game {}", sessionInfo.getPlayerId(), sessionInfo.getGameId());

                try {
                    Game updatedGame = gameService.forfeitGame(sessionInfo.getGameId(), sessionInfo.getPlayerId());

                    GameUpdateDTO gameUpdate = GameUpdateDTO.fromEntity(updatedGame);
                    String destination = "/topic/game/" + updatedGame.getGameId();
                    messagingTemplate.convertAndSend(destination, gameUpdate);

                    log.info("Game {} updated (forfeit) and broadcasted.", updatedGame.getGameId());

                } catch (Exception e) {
                    log.warn("Unable to process forfeit for game {}: {}", sessionInfo.getGameId(), e.getMessage());
                }
            }
            else {
                log.warn("Player {} disconnected from LOBBY (was in queue). Notifying matchmaking-service.", sessionInfo.getPlayerId());
                
                PlayerDisconnectEvent disconnectEvent = new PlayerDisconnectEvent(sessionInfo.getPlayerId());
                kafkaTemplate.send(KafkaConfig.PLAYER_DISCONNECTED_TOPIC, disconnectEvent.getPlayerId().toString(), disconnectEvent);
            }
        } else {
            log.info("Disconnected session {} was not registered in any game.", sessionId);
        }
    }
}