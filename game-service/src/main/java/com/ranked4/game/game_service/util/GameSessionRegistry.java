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
        private final UUID gameId;
        private final UUID playerId;

        public PlayerSessionInfo(UUID gameId, UUID playerId) {
            this.gameId = gameId;
            this.playerId = playerId;
        }

        public UUID getGameId() {
            return gameId;
        }

        public UUID getPlayerId() {
            return playerId;
        }
    }

    private final Map<String, PlayerSessionInfo> activeSessions = new ConcurrentHashMap<>();

    public void registerSession(String sessionId, UUID gameId, UUID playerId) {
        log.info("Enregistrement de la session {} pour le joueur {} dans la partie {}", sessionId, playerId, gameId);
        activeSessions.put(sessionId, new PlayerSessionInfo(gameId, playerId));
    }

    public PlayerSessionInfo unregisterSession(String sessionId) {
        PlayerSessionInfo info = activeSessions.remove(sessionId);
        if (info != null) {
            log.info("Session {} (Joueur {}) supprimée du registre.", sessionId, info.getPlayerId());
        }
        return info;
    }
}