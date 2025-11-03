package com.ranked4.game.game_service.dto;

import java.util.UUID;

public class MatchFoundEvent {

    private UUID matchId;
    private UUID playerOneId;
    private UUID playerTwoId;

    public UUID getMatchId() {
        return matchId;
    }

    public void setMatchId(UUID matchId) {
        this.matchId = matchId;
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

    @Override
    public String toString() {
        return "MatchFoundEvent{" +
                "matchId=" + matchId +
                ", playerOneId=" + playerOneId +
                ", playerTwoId=" + playerTwoId +
                '}';
    }
}