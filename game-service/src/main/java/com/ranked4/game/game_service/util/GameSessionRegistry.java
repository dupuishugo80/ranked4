package com.ranked4.game.game_service.util;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class GameSessionRegistry {

    private static final Logger log = LoggerFactory.getLogger(GameSessionRegistry.class);

    public static class PlayerSessionInfo {
        private UUID gameId;
        private final UUID playerId;

        public PlayerSessionInfo(UUID gameId, UUID playerId) {
            this.gameId = gameId;
            this.playerId = playerId;
        }

        public PlayerSessionInfo(UUID playerId) {
            this.playerId = playerId;
            this.gameId = null;
        }

        public UUID getGameId() {
            return gameId;
        }

        public UUID getPlayerId() {
            return playerId;
        }

        public void setGameId(UUID gameId) {
            this.gameId = gameId;
        }
        
        public boolean isInGame() {
            return this.gameId != null;
        }
    }

    public void registerLobbySession(String sessionId, UUID playerId) {
        activeSessions.putIfAbsent(sessionId, new PlayerSessionInfo(playerId));
        log.info("Registering LOBBY session {} for player {}.", sessionId, playerId);
    }

    public void registerGameSession(String sessionId, UUID gameId, UUID playerId) {
        PlayerSessionInfo info = activeSessions.get(sessionId);
        
        if (info != null && info.getPlayerId().equals(playerId)) {
            log.info("Updating session {}: assigning to game {}", sessionId, gameId);
            info.setGameId(gameId);
        } else {
            log.warn("Direct save of GAME session {} for player {} (game {})", sessionId, playerId, gameId);
            activeSessions.put(sessionId, new PlayerSessionInfo(gameId, playerId));
        }
    }

    private final Map<String, PlayerSessionInfo> activeSessions = new ConcurrentHashMap<>();

    public void registerSession(String sessionId, UUID gameId, UUID playerId) {
        log.info("Saving session {} for player {} in game {}", sessionId, playerId, gameId);
        activeSessions.put(sessionId, new PlayerSessionInfo(gameId, playerId));
    }

    public PlayerSessionInfo unregisterSession(String sessionId) {
        PlayerSessionInfo info = activeSessions.remove(sessionId);
        if (info != null) {
            log.info("Session {} (Player {}) removed from the registry.", sessionId, info.getPlayerId());
        }
        return info;
    }
}