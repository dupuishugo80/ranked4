package com.ranked4.matchmaking.matchmaking_service.dto;

import java.io.Serializable;
import java.util.UUID;

public class MatchmakingRequest implements Serializable {

    private UUID userId;
    private String displayName;
    private int elo;

    public MatchmakingRequest() {
    }

    public MatchmakingRequest(UUID userId, String displayName, int elo) {
        this.userId = userId;
        this.displayName = displayName;
        this.elo = elo;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public int getElo() {
        return elo;
    }

    public void setElo(int elo) {
        this.elo = elo;
    }
}