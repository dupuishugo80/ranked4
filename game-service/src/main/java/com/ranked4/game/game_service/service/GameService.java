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

import com.ranked4.game.game_service.dto.AiMoveRequest;
import com.ranked4.game.game_service.dto.AiMoveResponse;
import com.ranked4.game.game_service.dto.GameFinishedEvent;
import com.ranked4.game.game_service.dto.GameHistoryDTO;
import com.ranked4.game.game_service.dto.GameUpdateDTO;
import com.ranked4.game.game_service.dto.PlayerInfoDTO;
import com.ranked4.game.game_service.dto.UserProfileDataDTO;
import com.ranked4.game.game_service.model.Disc;
import com.ranked4.game.game_service.model.Game;
import com.ranked4.game.game_service.model.GameStatus;
import com.ranked4.game.game_service.model.GameType;
import com.ranked4.game.game_service.model.Move;
import com.ranked4.game.game_service.repository.GameRepository;
import com.ranked4.game.game_service.repository.MoveRepository;
import com.ranked4.game.game_service.util.KafkaConfig;

@Service
public class GameService {

    private static final Logger log = LoggerFactory.getLogger(GameService.class);
    private static final UUID AI_PLAYER_UUID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final String AI_DISPLAY_NAME = "IA";
    private static final String AI_AVATAR_URL = "https://img.freepik.com/vecteurs-libre/robot-vectoriel-graident-ai_78370-4114.jpg?semt=ais_se_enriched&w=740&q=80";

    private final GameRepository gameRepository;
    private final MoveRepository moveRepository;

    private final KafkaTemplate<String, GameFinishedEvent> kafkaTemplate;

    private final RestTemplate restTemplate;
    private final String USER_PROFILE_SERVICE_URL = "http://userprofile-service:8080/api/profiles/fullprofilesbyids";
    private final String AI_SERVICE_URL = "http://ai-service:8080/api/ai/next-move";

    public GameService(GameRepository gameRepository, MoveRepository moveRepository,
            KafkaTemplate<String, GameFinishedEvent> kafkaTemplate, RestTemplate restTemplate) {
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
    public Game createPveGame(UUID gameId, UUID playerId, int difficulty) {
        log.info("Creating PVE game (ID: {}) for player {} with difficulty {}", gameId, playerId, difficulty);

        if (gameRepository.existsById(gameId)) {
            log.warn("Attempting to create a game that already exists: {}", gameId);
            return gameRepository.findById(gameId).get();
        }

        Game game = new Game();
        game.startGame(gameId, playerId, AI_PLAYER_UUID);
        game.setRanked(false);
        game.setOrigin("PVE");
        game.setGameType(com.ranked4.game.game_service.model.GameType.PVE);
        game.setAiDifficulty(difficulty);

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
                    updatedGame.getOrigin(),
                    updatedGame.getGameType());
            event.setAiDifficulty(updatedGame.getAiDifficulty());

            try {
                kafkaTemplate.send(KafkaConfig.GAME_FINISHED_TOPIC, event.getGameId().toString(), event);
                log.info("GameFinishedEvent sent to Kafka for game {} (aiDifficulty: {})", gameId,
                        updatedGame.getAiDifficulty());
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
            throw new IllegalStateException(
                    "Le joueur déconnecté " + disconnectedPlayerId + " n'appartient pas à la partie " + gameId);
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
                finishedGame.getOrigin(),
                finishedGame.getGameType());
        event.setAiDifficulty(finishedGame.getAiDifficulty());

        try {
            kafkaTemplate.send(KafkaConfig.GAME_FINISHED_TOPIC, event.getGameId().toString(), event);
            log.info("Événement GameFinishedEvent envoyé sur Kafka pour la partie {} (aiDifficulty: {})",
                    finishedGame.getGameId(), finishedGame.getAiDifficulty());
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
                .filter(id -> !AI_PLAYER_UUID.equals(id))
                .collect(Collectors.toSet());
        Map<UUID, PlayerInfoDTO> infoMap = getPlayerInfoMap(playerIds);
        return games.stream()
                .map(game -> {
                    PlayerInfoDTO p1Info;
                    PlayerInfoDTO p2Info;

                    if (AI_PLAYER_UUID.equals(game.getPlayerOneId())) {
                        p1Info = createAiPlayerInfo();
                    } else {
                        p1Info = infoMap.get(game.getPlayerOneId());
                    }

                    if (AI_PLAYER_UUID.equals(game.getPlayerTwoId())) {
                        p2Info = createAiPlayerInfo();
                    } else {
                        p2Info = infoMap.get(game.getPlayerTwoId());
                    }

                    String p1Name = (p1Info != null && p1Info.displayName() != null)
                            ? p1Info.displayName()
                            : "Inconnu";

                    String p2Name = (p2Info != null && p2Info.displayName() != null)
                            ? p2Info.displayName()
                            : "Inconnu";
                    return new GameHistoryDTO(
                            game.getGameId(),
                            game.getPlayerOneId(),
                            p1Name,
                            game.getPlayerTwoId(),
                            p2Name,
                            game.getWinner(),
                            game.getCreatedAt());
                })
                .collect(Collectors.toList());
    }

    public Map<UUID, PlayerInfoDTO> getPlayerInfoMap(Set<UUID> playerIds) {
        if (playerIds.isEmpty()) {
            return Map.of();
        }

        try {
            ParameterizedTypeReference<List<UserProfileDataDTO>> responseType = new ParameterizedTypeReference<>() {
            };

            List<UserProfileDataDTO> profiles = restTemplate.exchange(
                    USER_PROFILE_SERVICE_URL,
                    HttpMethod.POST,
                    new org.springframework.http.HttpEntity<>(playerIds),
                    responseType).getBody();

            if (profiles == null) {
                return Map.of();
            }

            return profiles.stream()
                    .collect(Collectors.toMap(
                            UserProfileDataDTO::userId,
                            UserProfileDataDTO::toPlayerInfoDTO));

        } catch (Exception e) {
            log.error("Impossible de récupérer les profils utilisateur", e);
            return Map.of();
        }
    }

    public GameUpdateDTO createGameUpdateDTO(Game game) {
        PlayerInfoDTO p1Info;
        PlayerInfoDTO p2Info;

        if (AI_PLAYER_UUID.equals(game.getPlayerOneId())) {
            p1Info = createAiPlayerInfo();
            p2Info = getPlayerInfo(game.getPlayerTwoId());
        } else if (AI_PLAYER_UUID.equals(game.getPlayerTwoId())) {
            p1Info = getPlayerInfo(game.getPlayerOneId());
            p2Info = createAiPlayerInfo();
        } else {
            Set<UUID> playerIds = Set.of(game.getPlayerOneId(), game.getPlayerTwoId());
            Map<UUID, PlayerInfoDTO> infoMap = getPlayerInfoMap(playerIds);
            p1Info = infoMap.get(game.getPlayerOneId());
            p2Info = infoMap.get(game.getPlayerTwoId());

            if (p1Info == null) {
                p1Info = new PlayerInfoDTO(game.getPlayerOneId(), "Unknown", null, 0, null);
            }
            if (p2Info == null) {
                p2Info = new PlayerInfoDTO(game.getPlayerTwoId(), "Unknown", null, 0, null);
            }
        }

        return new GameUpdateDTO(game, p1Info, p2Info);
    }

    private PlayerInfoDTO createAiPlayerInfo() {
        return new PlayerInfoDTO(AI_PLAYER_UUID, AI_DISPLAY_NAME, AI_AVATAR_URL, 0, null);
    }

    private PlayerInfoDTO getPlayerInfo(UUID playerId) {
        Set<UUID> playerIds = Set.of(playerId);
        Map<UUID, PlayerInfoDTO> infoMap = getPlayerInfoMap(playerIds);
        PlayerInfoDTO playerInfo = infoMap.get(playerId);

        if (playerInfo == null) {
            playerInfo = new PlayerInfoDTO(playerId, "Unknown", null, 0, null);
        }

        return playerInfo;
    }

    public int getAiMove(Game game) {
        if (game.getGameType() != GameType.PVE) {
            throw new IllegalStateException("Cannot get AI move for non-PVE game");
        }

        Disc aiDisc = game.getNextPlayer();
        int aiPlayerId = aiDisc.getValue();

        AiMoveRequest request = new AiMoveRequest(
                game.getBoardState(),
                game.getAiDifficulty(),
                aiPlayerId);

        try {
            AiMoveResponse response = restTemplate.postForObject(
                    AI_SERVICE_URL,
                    request,
                    AiMoveResponse.class);

            if (response == null) {
                throw new IllegalStateException("AI service returned null response");
            }

            return response.column();
        } catch (Exception e) {
            log.error("Error calling AI service for game {}", game.getGameId(), e);
            throw new IllegalStateException("Failed to get AI move", e);
        }
    }

    @Transactional
    public Game applyAiMove(UUID gameId) {
        Game game = getGameState(gameId);

        if (game.getGameType() != GameType.PVE || game.getStatus() != GameStatus.IN_PROGRESS) {
            return game;
        }

        int aiColumn = getAiMove(game);
        return applyMove(gameId, AI_PLAYER_UUID, aiColumn);
    }
}