package com.ranked4.game.game_service.service;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ranked4.game.game_service.dto.GameFinishedEvent;
import com.ranked4.game.game_service.model.Disc;
import com.ranked4.game.game_service.model.Game;
import com.ranked4.game.game_service.model.GameStatus;
import com.ranked4.game.game_service.model.Move;
import com.ranked4.game.game_service.repository.GameRepository;
import com.ranked4.game.game_service.repository.MoveRepository;
import com.ranked4.game.game_service.util.KafkaConfig;

@Service
public class GameService {

    private static final Logger log = LoggerFactory.getLogger(GameService.class);

    private final GameRepository gameRepository;
    private final MoveRepository moveRepository;

    private final KafkaTemplate<String, GameFinishedEvent> kafkaTemplate;

    public GameService(GameRepository gameRepository, MoveRepository moveRepository, KafkaTemplate<String, GameFinishedEvent> kafkaTemplate) {
        this.gameRepository = gameRepository;
        this.moveRepository = moveRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Transactional
    public Game createGame(UUID gameId, UUID playerOneId, UUID playerTwoId) {
        log.info("Creating new game (ID: {}) for {} vs {}", gameId, playerOneId, playerTwoId);
        
        if (gameRepository.existsById(gameId)) {
            log.warn("Attempting to create a game that already exists: {}", gameId);
            return gameRepository.findById(gameId).get();
        }

        Game game = new Game();
        game.startGame(gameId, playerOneId, playerTwoId); 
        
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

            GameFinishedEvent event = new GameFinishedEvent(
                updatedGame.getGameId(),
                updatedGame.getPlayerOneId(),
                updatedGame.getPlayerTwoId(),
                updatedGame.getWinner()
            );
            
            try {
                kafkaTemplate.send(KafkaConfig.GAME_FINISHED_TOPIC, event.getGameId().toString(), event);
                log.info("GameFinishedEvent sent to Kafka for game {}", gameId);
            } catch (Exception e) {
                log.error("Error sending GameFinishedEvent to Kafka", e);
            }
        }

        return updatedGame;
    }

    @Transactional
    public Game forfeitGame(UUID gameId, UUID disconnectedPlayerId) {
        log.warn("Traitement du forfait pour le joueur {} dans la partie {}", disconnectedPlayerId, gameId);
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new IllegalStateException("Partie non trouvée: " + gameId));

        if (game.getStatus() == GameStatus.FINISHED) {
            log.info("La partie {} est déjà terminée. Aucun forfait traité.", gameId);
            return game;
        }

        Disc forfeitingDisc;
        if (disconnectedPlayerId.equals(game.getPlayerOneId())) {
            forfeitingDisc = Disc.PLAYER_ONE;
        } else if (disconnectedPlayerId.equals(game.getPlayerTwoId())) {
            forfeitingDisc = Disc.PLAYER_TWO;
        } else {
            throw new IllegalStateException("Le joueur déconnecté " + disconnectedPlayerId + " n'appartient pas à la partie " + gameId);
        }

        game.forfeit(forfeitingDisc);
        Game updatedGame = gameRepository.save(game);

        sendGameFinishedEvent(updatedGame);

        return updatedGame;
    }

    private void sendGameFinishedEvent(Game finishedGame) {
        GameFinishedEvent event = new GameFinishedEvent(
            finishedGame.getGameId(),
            finishedGame.getPlayerOneId(),
            finishedGame.getPlayerTwoId(),
            finishedGame.getWinner()
        );
        
        try {
            kafkaTemplate.send(KafkaConfig.GAME_FINISHED_TOPIC, event.getGameId().toString(), event);
            log.info("Événement GameFinishedEvent envoyé sur Kafka pour la partie {}", finishedGame.getGameId());
        } catch (Exception e) {
            log.error("Erreur lors de l'envoi de l'événement GameFinishedEvent sur Kafka", e);
        }
    }

    @Transactional(readOnly = true)
    public Game getGameState(UUID gameId) {
        return gameRepository.findById(gameId)
                .orElseThrow(() -> new IllegalStateException("Game not found: " + gameId));
    }
}