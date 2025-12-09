package com.ranked4.game.game_service.service;

import com.ranked4.game.game_service.model.Game;
import com.ranked4.game.game_service.model.GameStatus;
import com.ranked4.game.game_service.repository.GameRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
public class TurnTimerService {

    private static final Logger logger = LoggerFactory.getLogger(TurnTimerService.class);
    private static final long TURN_TIMEOUT_SECONDS = 60;

    private final GameRepository gameRepository;
    private final GameService gameService;
    private com.ranked4.game.game_service.controller.GameSocketController gameSocketController;

    public TurnTimerService(GameRepository gameRepository, GameService gameService) {
        this.gameRepository = gameRepository;
        this.gameService = gameService;
    }

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    public void setGameSocketController(com.ranked4.game.game_service.controller.GameSocketController gameSocketController) {
        this.gameSocketController = gameSocketController;
    }

    @Scheduled(fixedRate = 2000)
    public void checkExpiredTurns() {
        Instant threshold = Instant.now().minusSeconds(TURN_TIMEOUT_SECONDS);
        List<Game> expiredGames = gameRepository.findGamesWithExpiredTurns(GameStatus.IN_PROGRESS, threshold);

        if (!expiredGames.isEmpty()) {
            logger.info("Found {} games with expired turns", expiredGames.size());
        }

        for (Game game : expiredGames) {
            logger.info("Turn timeout for game {} - auto-playing for player {}",
                    game.getGameId(), game.getNextPlayer());
            autoPlayMove(game);
        }
    }

    @Transactional
    public void autoPlayMove(Game game) {
        try {
            logger.info("Starting auto-play for game {}, current player: {}, playerId: {}",
                    game.getGameId(), game.getNextPlayer(), game.getNextPlayerId());

            int firstAvailableColumn = findFirstAvailableColumn(game);
            logger.info("First available column: {}", firstAvailableColumn);

            if (firstAvailableColumn != -1) {
                logger.info("Calling gameService.applyMove for game {}, playerId {}, column {}",
                        game.getGameId(), game.getNextPlayerId(), firstAvailableColumn);

                com.ranked4.game.game_service.model.Game updatedGame = gameService.applyMove(
                    game.getGameId(),
                    game.getNextPlayerId(),
                    firstAvailableColumn
                );

                logger.info("Auto-play successful for game {}", game.getGameId());

                if (gameSocketController != null) {
                    logger.info("Broadcasting game update after auto-play for game {}", game.getGameId());
                    gameSocketController.broadcastGameUpdate(game.getGameId());

                    if (updatedGame.getGameType() == com.ranked4.game.game_service.model.GameType.PVE
                        && updatedGame.getStatus() == com.ranked4.game.game_service.model.GameStatus.IN_PROGRESS) {
                        logger.info("PVE game detected, triggering AI move for game {}", game.getGameId());

                        try {
                            Thread.sleep(500);
                            gameService.applyAiMove(game.getGameId());
                            gameSocketController.broadcastGameUpdate(game.getGameId());
                            logger.info("AI move completed for game {}", game.getGameId());
                        } catch (Exception aiException) {
                            logger.error("Error during AI move after auto-play for game {}", game.getGameId(), aiException);
                        }
                    }
                } else {
                    logger.warn("GameSocketController not available for broadcasting");
                }
            } else {
                logger.warn("No available column found for auto-play in game {} - board might be full", game.getGameId());
            }
        } catch (Exception e) {
            logger.error("CRITICAL ERROR during auto-play for game {}: {} - Stack trace:",
                    game.getGameId(), e.getMessage(), e);

            try {
                Game freshGame = gameRepository.findById(game.getGameId()).orElse(null);
                if (freshGame != null) {
                    freshGame.setTurnStartTime(Instant.now().plusSeconds(TURN_TIMEOUT_SECONDS));
                    gameRepository.save(freshGame);
                    logger.info("Reset turn timer for game {} to avoid infinite loop", game.getGameId());
                }
            } catch (Exception saveError) {
                logger.error("Failed to reset timer for game {}: {}", game.getGameId(), saveError.getMessage());
            }
        }
    }

    private int findFirstAvailableColumn(Game game) {
        String boardState = game.getBoardState();

        for (int col = 0; col < 7; col++) {
            if (isColumnAvailable(boardState, col)) {
                return col;
            }
        }

        return -1;
    }

    private boolean isColumnAvailable(String boardState, int column) {
        for (int row = 0; row < 6; row++) {
            int index = row * 7 + column;
            if (boardState.charAt(index) == '0') {
                return true;
            }
        }
        return false;
    }
}
