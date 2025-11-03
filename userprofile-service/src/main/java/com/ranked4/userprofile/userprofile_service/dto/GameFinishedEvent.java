package com.ranked4.userprofile.userprofile_service.dto;

import java.util.UUID;

public class GameFinishedEvent {

    private UUID gameId;
    private UUID playerOneId;
    private UUID playerTwoId;
    private String winner;

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

    @Override
    public String toString() {
        return "GameFinishedEvent{" +
                "gameId=" + gameId +
                ", playerOneId=" + playerOneId +
                ", playerTwoId=" + playerTwoId +
                ", winner='" + winner + '\'' +
                '}';
    }
}