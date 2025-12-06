package com.ranked4.userprofile.userprofile_service.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.ranked4.userprofile.userprofile_service.model.UserProfile;

public record MyUserProfileDTO(
    UUID userId,
    String displayName,
    String avatarUrl,
    int elo,
    int gamesPlayed,
    int wins,
    int losses,
    int draws,
    int gold,
    DiscCustomizationDTO equippedDisc,
    List<DiscCustomizationDTO> ownedDiscs,
    Instant createdAt,
    Instant updatedAt
) {
    public MyUserProfileDTO(UserProfile entity) {
        this(
            entity.getUserId(),
            entity.getDisplayName(),
            entity.getAvatarUrl(),
            entity.getElo(),
            entity.getGamesPlayed(),
            entity.getWins(),
            entity.getLosses(),
            entity.getDraws(),
            entity.getGold(),
            DiscCustomizationDTO.fromEntity(entity.getEquippedDisc()),
            entity.getOwnedDiscs().stream()
                .map(DiscCustomizationDTO::fromEntity)
                .toList(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }

    public static MyUserProfileDTO fromEntity(UserProfile entity) {
        if (entity == null) {
            return null;
        }
        return new MyUserProfileDTO(entity);
    }
}