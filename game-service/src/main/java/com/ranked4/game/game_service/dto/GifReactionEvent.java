package com.ranked4.game.game_service.dto;

import java.util.UUID;

public class GifReactionEvent {

    private UUID gameId;
    private UUID playerId;
    private String gifCode;
    private String assetPath;
    private long timestamp;

    public UUID getGameId() {
        return gameId;
    }

    public void setGameId(UUID gameId) {
        this.gameId = gameId;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public void setPlayerId(UUID playerId) {
        this.playerId = playerId;
    }

    public String getGifCode() {
        return gifCode;
    }

    public void setGifCode(String gifCode) {
        this.gifCode = gifCode;
    }

    public String getAssetPath() {
        return assetPath;
    }

    public void setAssetPath(String assetPath) {
        this.assetPath = assetPath;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}