package com.ranked4.game.game_service.dto;

import java.util.UUID;

public record UserProfileDataDTO(
    UUID userId,
    String displayName,
    String avatarUrl,
    int elo,
    DiscCustomizationDTO equippedDisc
) {
    public PlayerInfoDTO toPlayerInfoDTO() {
        return new PlayerInfoDTO(
            this.userId,
            this.displayName,
            this.avatarUrl,
            this.elo,
            this.equippedDisc
        );
    }
}