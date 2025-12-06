package com.ranked4.userprofile.userprofile_service.dto;

import java.time.Instant;
import java.util.UUID;

import com.ranked4.userprofile.userprofile_service.model.UserProfile;

public record UserProfileDTO(
    UUID userId,
    String displayName,
    String avatarUrl,
    int elo,
    int gamesPlayed,
    int wins,
    int losses,
    int draws,
    DiscCustomizationDTO equippedDisc,
    Instant createdAt,
    Instant updatedAt
) {
    public UserProfileDTO(UserProfile entity) {
        this(
            entity.getUserId(),
            entity.getDisplayName(),
            entity.getAvatarUrl(),
            entity.getElo(),
            entity.getGamesPlayed(),
            entity.getWins(),
            entity.getLosses(),
            entity.getDraws(),
            DiscCustomizationDTO.fromEntity(entity.getEquippedDisc()),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }

    public static UserProfileDTO fromEntity(UserProfile entity) {
        if (entity == null) {
            return null;
        }
        return new UserProfileDTO(entity);
    }
}