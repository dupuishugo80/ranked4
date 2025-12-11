package com.ranked4.userprofile.userprofile_service.dto;

import java.util.UUID;

import com.ranked4.userprofile.userprofile_service.model.UserProfile;

public record LeaderboardEntryDTO(
    UUID userId,
    String displayName,
    String avatarUrl,
    int elo,
    int wins,
    int losses,
    int draws,
    int rank
) {
    public LeaderboardEntryDTO(UserProfile entity) {
        this(
            entity.getUserId(),
            entity.getDisplayName(),
            entity.getAvatarUrl(),
            entity.getElo(),
            entity.getWins(),
            entity.getLosses(),
            entity.getDraws(),
            0
        );
    }
}