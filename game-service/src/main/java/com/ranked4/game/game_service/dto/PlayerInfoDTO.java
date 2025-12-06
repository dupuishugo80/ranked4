package com.ranked4.game.game_service.dto;

import java.util.UUID;

public record PlayerInfoDTO(
    UUID userId,
    String displayName,
    String avatarUrl,
    int elo,
    DiscCustomizationDTO disc
) {}
