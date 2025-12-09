package com.ranked4.game.game_service.controller;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import com.ranked4.game.game_service.dto.GameUpdateDTO;
import com.ranked4.game.game_service.dto.PlayerInfoDTO;
import com.ranked4.game.game_service.dto.PlayerJoinDTO;
import com.ranked4.game.game_service.dto.PlayerMoveDTO;
import com.ranked4.game.game_service.model.Game;
import com.ranked4.game.game_service.model.GameStatus;
import com.ranked4.game.game_service.model.GameType;
import com.ranked4.game.game_service.service.GameService;
import com.ranked4.game.game_service.util.GameSessionRegistry;

@Controller
public class GameSocketController {

    private static final Logger log = LoggerFactory.getLogger(GameSocketController.class);

    private final GameService gameService;
    private final SimpMessagingTemplate messagingTemplate;
    private final GameSessionRegistry gameSessionRegistry;

    public GameSocketController(GameService gameService, SimpMessagingTemplate messagingTemplate,
            GameSessionRegistry gameSessionRegistry) {
        this.gameService = gameService;
        this.messagingTemplate = messagingTemplate;
        this.gameSessionRegistry = gameSessionRegistry;
    }

    @MessageMapping("/game.move/{gameId}")
    public void handleMove(@DestinationVariable UUID gameId, PlayerMoveDTO move) {
        GameUpdateDTO gameUpdate;
        Game game;
        try {
            game = gameService.applyMove(
                    gameId,
                    move.playerId(),
                    move.column());
            gameUpdate = createGameUpdateDTO(game);

            String destination = "/topic/game/" + gameId;
            messagingTemplate.convertAndSend(destination, gameUpdate);

            if (game.getGameType() == GameType.PVE && game.getStatus() == GameStatus.IN_PROGRESS) {
                try {
                    Thread.sleep(500);
                    game = gameService.applyAiMove(gameId);
                    gameUpdate = createGameUpdateDTO(game);
                    messagingTemplate.convertAndSend(destination, gameUpdate);
                } catch (Exception aiException) {
                    log.error("Error applying AI move for game {}", gameId, aiException);
                    gameUpdate = createGameUpdateDTO(game).withError("AI move failed: " + aiException.getMessage());
                    messagingTemplate.convertAndSend(destination, gameUpdate);
                }
            }
        } catch (IllegalStateException e) {
            game = gameService.getGameState(gameId);
            gameUpdate = createGameUpdateDTO(game).withError(e.getMessage());
            String destination = "/topic/game/" + gameId;
            messagingTemplate.convertAndSend(destination, gameUpdate);
        }
    }

    @MessageMapping("/game.join/{gameId}")
    public void joinGame(@DestinationVariable UUID gameId, PlayerJoinDTO joinMessage,
            SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        UUID playerId = joinMessage.playerId();

        log.info("Player {} (Session: {}) has joined the room for game {}", playerId, sessionId, gameId);

        gameSessionRegistry.registerSession(sessionId, gameId, playerId);

        Game currentGame = gameService.getGameState(gameId);
        GameUpdateDTO gameUpdate = createGameUpdateDTO(currentGame);

        String destination = "/topic/game/" + gameId;
        messagingTemplate.convertAndSend(destination, gameUpdate);

        if (currentGame.getGameType() == GameType.PVE &&
                currentGame.getStatus() == GameStatus.IN_PROGRESS &&
                currentGame.isAiOpponent(currentGame.getNextPlayerId())) {

            log.info("AI's turn detected in PVE game {}, triggering AI move", gameId);

            try {
                Thread.sleep(500);
                Game updatedGame = gameService.applyAiMove(gameId);
                GameUpdateDTO aiMoveUpdate = createGameUpdateDTO(updatedGame);
                messagingTemplate.convertAndSend(destination, aiMoveUpdate);
            } catch (Exception aiException) {
                log.error("Error applying initial AI move for game {}", gameId, aiException);
                gameUpdate = createGameUpdateDTO(currentGame).withError("AI move failed: " + aiException.getMessage());
                messagingTemplate.convertAndSend(destination, gameUpdate);
            }
        }
    }

    private GameUpdateDTO createGameUpdateDTO(Game game) {
        Set<UUID> playerIds = Set.of(game.getPlayerOneId(), game.getPlayerTwoId());
        Map<UUID, PlayerInfoDTO> infoMap = gameService.getPlayerInfoMap(playerIds);

        PlayerInfoDTO p1Info = infoMap.get(game.getPlayerOneId());
        PlayerInfoDTO p2Info = infoMap.get(game.getPlayerTwoId());

        if (p1Info == null)
            p1Info = new PlayerInfoDTO(game.getPlayerOneId(), "Unknown", null, 0, null);
        if (p2Info == null)
            p2Info = new PlayerInfoDTO(game.getPlayerTwoId(), "Unknown", null, 0, null);

        return new GameUpdateDTO(game, p1Info, p2Info);
    }

    @MessageMapping("/lobby.register")
    public void registerLobbyPresence(PlayerJoinDTO joinMessage, SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        UUID playerId = joinMessage.playerId();

        if (playerId == null || sessionId == null) {
            log.warn("Invalid attempt to register to the lobby.");
            return;
        }

        gameSessionRegistry.registerLobbySession(sessionId, playerId);
    }

    public void broadcastGameUpdate(UUID gameId) {
        try {
            Game game = gameService.getGameState(gameId);
            GameUpdateDTO gameUpdate = createGameUpdateDTO(game);
            String destination = "/topic/game/" + gameId;
            messagingTemplate.convertAndSend(destination, gameUpdate);
            log.info("Broadcasted game update for game {}", gameId);
        } catch (Exception e) {
            log.error("Error broadcasting game update for game {}", gameId, e);
        }
    }
}