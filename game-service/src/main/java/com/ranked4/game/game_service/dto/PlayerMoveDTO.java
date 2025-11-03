package com.ranked4.game.game_service.dto;

import java.util.UUID;

public class PlayerMoveDTO {

    private UUID gameId;
    private UUID playerId;
    private int column;

    public PlayerMoveDTO() {
    }

    public UUID getGameId() {
        return gameId;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public int getColumn() {
        return column;
    }

    public void setGameId(UUID gameId) {
        this.gameId = gameId;
    }

    public void setPlayerId(UUID playerId) {
        this.playerId = playerId;
    }

    public void setColumn(int column) {
        this.column = column;
    }
}
