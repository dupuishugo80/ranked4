package com.ranked4.matchmaking.matchmaking_service.dto;

import java.util.UUID;

public class UserProfileDTO {

    private UUID userId;
    private String displayName;
    private int elo;

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

    @Override
    public String toString() {
        return "UserProfileDTO{" +
                "userId=" + userId +
                ", displayName='" + displayName + '\'' +
                ", elo=" + elo +
                '}';
    }
}