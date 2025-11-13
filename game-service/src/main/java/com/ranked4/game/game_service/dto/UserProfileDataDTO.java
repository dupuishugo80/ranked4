package com.ranked4.game.game_service.dto;

import java.util.UUID;

public class UserProfileDataDTO {
    private UUID userId;
    private String displayName;
    private String avatarUrl;
    private int elo;
    private DiscCustomizationDTO equippedDisc;

    public PlayerInfoDTO toPlayerInfoDTO() {
        PlayerInfoDTO info = new PlayerInfoDTO();
        info.setUserId(this.userId);
        info.setDisplayName(this.displayName);
        info.setAvatarUrl(this.avatarUrl);
        info.setElo(this.elo);
        info.setDisc(this.equippedDisc);
        return info;
    }
    
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    public int getElo() { return elo; }
    public void setElo(int elo) { this.elo = elo; }
    public DiscCustomizationDTO getEquippedDisc() { return equippedDisc; }
    public void setEquippedDisc(DiscCustomizationDTO equippedDisc) { this.equippedDisc = equippedDisc; }
}