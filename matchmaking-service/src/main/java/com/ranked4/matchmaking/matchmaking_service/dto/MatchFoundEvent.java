package com.ranked4.matchmaking.matchmaking_service.dto;

import java.util.UUID;

public class MatchFoundEvent {

    private boolean ranked = true;
    private String origin = "RANKED";
    private UUID matchId;
    private UUID playerOneId;
    private UUID playerTwoId;

    public MatchFoundEvent() {
    }

    public MatchFoundEvent(UUID playerOneId, UUID playerTwoId) {
        this(playerOneId, playerTwoId, true, "RANKED");
    }

    public MatchFoundEvent(UUID playerOneId, UUID playerTwoId, boolean ranked, String origin) {
        this.matchId = UUID.randomUUID();
        this.playerOneId = playerOneId;
        this.playerTwoId = playerTwoId;
        this.ranked = ranked;
        this.origin = origin;
    }

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

    public UUID getPlayerTwoId() {
        return playerTwoId;
    }

    public void setPlayerTwoId(UUID playerTwoId) {
        this.playerTwoId = playerTwoId;
    }
}