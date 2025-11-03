package com.ranked4.game.game_service.service;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ranked4.game.game_service.model.Disc;
import com.ranked4.game.game_service.model.Game;
import com.ranked4.game.game_service.model.GameStatus;
import com.ranked4.game.game_service.model.Move;
import com.ranked4.game.game_service.repository.GameRepository;
import com.ranked4.game.game_service.repository.MoveRepository;

@Service
public class GameService {

    private static final Logger log = LoggerFactory.getLogger(GameService.class);

    private final GameRepository gameRepository;
    private final MoveRepository moveRepository;

    public GameService(GameRepository gameRepository, MoveRepository moveRepository) {
        this.gameRepository = gameRepository;
        this.moveRepository = moveRepository;
    }

    @Transactional
    public Game createGame(UUID playerOneId, UUID playerTwoId) {
        log.info("New game created: {} vs {}", playerOneId, playerTwoId);
        Game game = new Game();
        game.startGame(playerOneId, playerTwoId);
        
        return gameRepository.save(game);
    }

    @Transactional
    public Game applyMove(UUID gameId, UUID playerId, int column) {
        log.info("Attempting move: Game {}, Player {}, Column {}", gameId, playerId, column);

        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new IllegalStateException("Game not found: " + gameId));

        if (game.getStatus() != GameStatus.IN_PROGRESS) {
            throw new IllegalStateException("Game is not in progress.");
        }

        Disc playerDisc;
        if (playerId.equals(game.getPlayerOneId())) {
            playerDisc = Disc.PLAYER_ONE;
        } else if (playerId.equals(game.getPlayerTwoId())) {
            playerDisc = Disc.PLAYER_TWO;
        } else {
            throw new IllegalStateException("Player " + playerId + " is not part of this game.");
        }

        boolean moveSuccess = game.applyMove(column, playerDisc);

        if (!moveSuccess) {
            throw new IllegalStateException("Invalid move. Column " + column + " may be full.");
        }

        int moveNumber = moveRepository.findByGameGameIdOrderByMoveNumberAsc(gameId).size() + 1;
        Move move = new Move(game, playerDisc, column, moveNumber);
        moveRepository.save(move);

        Game updatedGame = gameRepository.save(game);

        if (updatedGame.getStatus() == GameStatus.FINISHED) {
            log.info("Game finished: {}. Winner: {}", gameId, updatedGame.getWinner());
        }

        return updatedGame;
    }

    @Transactional(readOnly = true)
    public Game getGameState(UUID gameId) {
        return gameRepository.findById(gameId)
                .orElseThrow(() -> new IllegalStateException("Game not found: " + gameId));
    }
}