package com.ranked4.game.game_service.controller;

import java.security.Principal;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

import com.ranked4.game.game_service.dto.ErrorDTO;
import com.ranked4.game.game_service.dto.GameUpdateDTO;
import com.ranked4.game.game_service.dto.PlayerMoveDTO;
import com.ranked4.game.game_service.model.Game;
import com.ranked4.game.game_service.service.GameService;

@Controller
public class GameSocketController {

    private static final Logger log = LoggerFactory.getLogger(GameSocketController.class);

    private final GameService gameService;
    private final SimpMessagingTemplate messagingTemplate;

    public GameSocketController(GameService gameService, SimpMessagingTemplate messagingTemplate) {
        this.gameService = gameService;
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/game.move/{gameId}")
    public void handleMove(@DestinationVariable UUID gameId, PlayerMoveDTO move) {
        log.info("Move received for game {}: Player {} plays column {}", gameId, move.getPlayerId(), move.getColumn());
        Game updatedGame = gameService.applyMove(
            gameId,
            move.getPlayerId(),
            move.getColumn()
        );

        GameUpdateDTO gameUpdate = GameUpdateDTO.fromEntity(updatedGame);

        String destination = "/topic/game/" + gameId;
        messagingTemplate.convertAndSend(destination, gameUpdate);

        log.info("Game {} state updated and broadcasted.", gameId);
    }

    @MessageMapping("/game.join/{gameId}")
    public void joinGame(@DestinationVariable UUID gameId, Principal principal) {
        log.info("Player {} joined the room for game {}", (principal != null ? principal.getName() : "unknown"), gameId);

        Game currentGame = gameService.getGameState(gameId);
        GameUpdateDTO gameUpdate = GameUpdateDTO.fromEntity(currentGame);
        
        String destination = "/topic/game/" + gameId;
        messagingTemplate.convertAndSend(destination, gameUpdate);
    }

    @MessageExceptionHandler
    @SendToUser("/queue/errors")
    public ErrorDTO handleException(Exception exception, Principal principal) {
        String username = (principal != null) ? principal.getName() : "unknown";
        String errorMessage = "Error: " + exception.getMessage();
        
        log.warn("Error for user {}: {}", username, errorMessage);

        return new ErrorDTO(errorMessage);
    }
}