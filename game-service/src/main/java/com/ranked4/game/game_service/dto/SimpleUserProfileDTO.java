package com.ranked4.game.game_service.dto;

import java.util.UUID;

public class SimpleUserProfileDTO {
    private UUID userId;
    private String displayName;

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
}