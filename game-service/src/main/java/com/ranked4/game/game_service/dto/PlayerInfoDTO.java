package com.ranked4.game.game_service.dto;

import java.util.UUID;

public class PlayerInfoDTO {
    private UUID userId;
    private String displayName;
    private String avatarUrl;
    private int elo;
    private DiscCustomizationDTO disc;

    public PlayerInfoDTO(UUID userId, String displayName, String avatarUrl, int elo, DiscCustomizationDTO disc) {
        this.userId = userId;
        this.displayName = displayName;
        this.avatarUrl = avatarUrl;
        this.elo = elo;
        this.disc = disc;
    }

    public PlayerInfoDTO() {}

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

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public int getElo() {
        return elo;
    }

    public void setElo(int elo) {
        this.elo = elo;
    }

    public DiscCustomizationDTO getDisc() {
        return disc;
    }

    public void setDisc(DiscCustomizationDTO disc) {
        this.disc = disc;
    } 
}
