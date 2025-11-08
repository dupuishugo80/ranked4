package com.ranked4.game.game_service.dto;

import java.util.UUID;

public class PlayerJoinDTO {

    private UUID playerId;

    public PlayerJoinDTO() {
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public void setPlayerId(UUID playerId) {
        this.playerId = playerId;
    }
}