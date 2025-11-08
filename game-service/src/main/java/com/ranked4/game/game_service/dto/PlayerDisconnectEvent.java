package com.ranked4.game.game_service.dto;

import java.util.UUID;

public class PlayerDisconnectEvent {
    
    private UUID playerId;

    public PlayerDisconnectEvent() {
    }

    public PlayerDisconnectEvent(UUID playerId) {
        this.playerId = playerId;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public void setPlayerId(UUID playerId) {
        this.playerId = playerId;
    }
}