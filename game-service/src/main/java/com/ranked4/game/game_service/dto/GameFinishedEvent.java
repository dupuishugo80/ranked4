package com.ranked4.game.game_service.dto;

import java.util.UUID;

import com.ranked4.game.game_service.model.Disc;

public class GameFinishedEvent {

    private UUID gameId;
    private UUID playerOneId;
    private UUID playerTwoId;
    private String winner;

    public GameFinishedEvent() {
    }

    public GameFinishedEvent(UUID gameId, UUID playerOneId, UUID playerTwoId, Disc winnerDisc) {
        this.gameId = gameId;
        this.playerOneId = playerOneId;
        this.playerTwoId = playerTwoId;
        
        if (winnerDisc == null) {
            this.winner = null;
        } else {
            this.winner = winnerDisc.name();
        }
    }

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
}