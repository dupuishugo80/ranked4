package com.ranked4.userprofile.userprofile_service.dto;

import java.util.UUID;

public class GameFinishedEvent {

    private UUID gameId;
    private UUID playerOneId;
    private UUID playerTwoId;
    private String winner;
    private boolean ranked = true;
    private String origin = "RANKED";
    private String gameType = "PVP_RANKED";
    private Integer aiDifficulty;

    public UUID getGameId() {
        return gameId;
    }

    public void setGameId(UUID gameId) {
        this.gameId = gameId;
    }

    public UUID getPlayerOneId() {
        return playerOneId;
    }

    public void setPlayerOneId(UUID playerOneId) {
        this.playerOneId = playerOneId;
    }

    public UUID getPlayerTwoId() {
        return playerTwoId;
    }

    public void setPlayerTwoId(UUID playerTwoId) {
        this.playerTwoId = playerTwoId;
    }

    public String getWinner() {
        return winner;
    }

    public void setWinner(String winner) {
        this.winner = winner;
    }

    public boolean isRanked() {
        return ranked;
    }

    public void setRanked(boolean ranked) {
        this.ranked = ranked;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public String getGameType() {
        return gameType;
    }

    public void setGameType(String gameType) {
        this.gameType = gameType;
    }

    public Integer getAiDifficulty() {
        return aiDifficulty;
    }

    public void setAiDifficulty(Integer aiDifficulty) {
        this.aiDifficulty = aiDifficulty;
    }

    @Override
    public String toString() {
        return "GameFinishedEvent{" +
                "gameId=" + gameId +
                ", playerOneId=" + playerOneId +
                ", playerTwoId=" + playerTwoId +
                ", winner='" + winner + '\'' +
                ", ranked=" + ranked +
                ", origin='" + origin + '\'' +
                ", gameType='" + gameType + '\'' +
                ", aiDifficulty=" + aiDifficulty +
                '}';
    }
}
