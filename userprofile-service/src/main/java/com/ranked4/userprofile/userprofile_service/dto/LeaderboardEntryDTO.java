package com.ranked4.userprofile.userprofile_service.dto;

import com.ranked4.userprofile.userprofile_service.model.UserProfile;

public record LeaderboardEntryDTO(
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