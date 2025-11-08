package com.ranked4.game.game_service.controller;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

import com.ranked4.game.game_service.dto.ErrorDTO;
import com.ranked4.game.game_service.dto.GameUpdateDTO;
import com.ranked4.game.game_service.dto.PlayerJoinDTO;
import com.ranked4.game.game_service.dto.PlayerMoveDTO;
import com.ranked4.game.game_service.model.Game;
import com.ranked4.game.game_service.service.GameService;
import com.ranked4.game.game_service.util.GameSessionRegistry;

@Controller
public class GameSocketController {

    private static final Logger log = LoggerFactory.getLogger(GameSocketController.class);

    private final GameService gameService;
    private final SimpMessagingTemplate messagingTemplate;
    private final GameSessionRegistry gameSessionRegistry;

    public GameSocketController(GameService gameService, SimpMessagingTemplate messagingTemplate, GameSessionRegistry gameSessionRegistry) {
        this.gameService = gameService;
        this.messagingTemplate = messagingTemplate;
        this.gameSessionRegistry = gameSessionRegistry;
    }

    @MessageMapping("/game.move/{gameId}")
    public void handleMove(@DestinationVariable UUID gameId, PlayerMoveDTO move) {
        GameUpdateDTO gameUpdate;
        try {
            log.info("Move received for game {}: Player {} plays column {}", gameId, move.getPlayerId(), move.getColumn());

            Game updatedGame = gameService.applyMove(
                gameId,
                move.getPlayerId(),
                move.getColumn()
            );

            gameUpdate = GameUpdateDTO.fromEntity(updatedGame);

            log.info("Game {} state updated and broadcasted", gameId);
        }
        catch (IllegalStateException e) {
            log.warn("Invalid move for game {}: {}", gameId, e.getMessage());
            
            Game currentGame = gameService.getGameState(gameId);
            gameUpdate = GameUpdateDTO.fromEntity(currentGame);
            
            gameUpdate.setError(e.getMessage());
        }

        String destination = "/topic/game/" + gameId;
        messagingTemplate.convertAndSend(destination, gameUpdate);
    }

    @MessageMapping("/game.join/{gameId}")
    public void joinGame(@DestinationVariable UUID gameId, PlayerJoinDTO joinMessage, SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        UUID playerId = joinMessage.getPlayerId();

        log.info("Player {} (Session: {}) has joined the room for game {}", playerId, sessionId, gameId);

        gameSessionRegistry.registerSession(sessionId, gameId, playerId);

        Game currentGame = gameService.getGameState(gameId);
        GameUpdateDTO gameUpdate = GameUpdateDTO.fromEntity(currentGame);
        
        String destination = "/topic/game/" + gameId;
        messagingTemplate.convertAndSend(destination, gameUpdate);
    }
}