package com.ranked4.game.game_service.service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import com.ranked4.game.game_service.dto.GameFinishedEvent;
import com.ranked4.game.game_service.dto.GameHistoryDTO;
import com.ranked4.game.game_service.dto.SimpleUserProfileDTO;
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

    private final RestTemplate restTemplate;
    private final String USER_PROFILE_SERVICE_URL = "http://userprofile-service:8080/api/profiles/usernamefromids";

    public GameService(GameRepository gameRepository, MoveRepository moveRepository, KafkaTemplate<String, GameFinishedEvent> kafkaTemplate, RestTemplate restTemplate) {
        this.gameRepository = gameRepository;
        this.moveRepository = moveRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.restTemplate = restTemplate;
    }

    @Transactional
    public Game createGame(UUID gameId, UUID playerOneId, UUID playerTwoId) {
        return createGame(gameId, playerOneId, playerTwoId, true, "RANKED");
    }

    @Transactional
    public Game createGame(UUID gameId, UUID playerOneId, UUID playerTwoId, boolean ranked, String origin) {
        log.info("Creating new game (ID: {}) for {} vs {}", gameId, playerOneId, playerTwoId);
        
        if (gameRepository.existsById(gameId)) {
            log.warn("Attempting to create a game that already exists: {}", gameId);
            return gameRepository.findById(gameId).get();
        }

        Game game = new Game();
        game.startGame(gameId, playerOneId, playerTwoId);
        game.setRanked(ranked);
        game.setOrigin(origin);

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
                updatedGame.getWinner(),
                updatedGame.isRanked(),
                updatedGame.getOrigin()
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
            finishedGame.getWinner(),
            finishedGame.isRanked(),
            finishedGame.getOrigin()
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

    @Transactional
    public Game cancelGameNoShow(UUID gameId) {
        Game game = gameRepository.findById(gameId)
            .orElseThrow(() -> new IllegalStateException("Game not found: " + gameId));

        if (!game.isRanked() || game.getStatus() != GameStatus.IN_PROGRESS) {
            return game;
        }

        log.warn("Cancelling ranked game {} due to missing player(s) after grace period.", gameId);
        game.setRanked(false);
        game.setOrigin("CANCELLED_NO_SHOW");
        game.setStatus(GameStatus.FINISHED);
        return gameRepository.save(game);
    }

@Transactional(readOnly = true)
    public List<GameHistoryDTO> getGameHistory() {        
        List<Game> games = gameRepository.findTop5ByRankedTrueAndStatusOrderByFinishedAtDesc(GameStatus.FINISHED);
        Set<UUID> playerIds = games.stream()
                .flatMap(game -> Stream.of(game.getPlayerOneId(), game.getPlayerTwoId()))
                .collect(Collectors.toSet());
        Map<UUID, String> nameMap = getPlayerNameMap(playerIds);
        return games.stream()
                .map(game -> new GameHistoryDTO(
                        game.getGameId(),
                        game.getPlayerOneId(),
                        nameMap.getOrDefault(game.getPlayerOneId(), "Inconnu"),
                        game.getPlayerTwoId(),
                        nameMap.getOrDefault(game.getPlayerTwoId(), "Inconnu"),
                        game.getWinner(),
                        game.getCreatedAt()
                ))
                .collect(Collectors.toList());
    }

    private Map<UUID, String> getPlayerNameMap(Set<UUID> playerIds) {
        if (playerIds.isEmpty()) {
            return Map.of();
        }

        try {
            ParameterizedTypeReference<List<SimpleUserProfileDTO>> responseType = 
                new ParameterizedTypeReference<>() {};

            List<SimpleUserProfileDTO> profiles = restTemplate.exchange(
                USER_PROFILE_SERVICE_URL,
                HttpMethod.POST,
                new org.springframework.http.HttpEntity<>(playerIds),
                responseType
            ).getBody();

            if (profiles == null) {
                return Map.of();
            }
            
            return profiles.stream()
                .collect(Collectors.toMap(SimpleUserProfileDTO::getUserId, SimpleUserProfileDTO::getDisplayName));

        } catch (Exception e) {
            log.error("Impossible de récupérer les profils utilisateur", e);
            return Map.of();
        }
    }
}