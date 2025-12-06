package com.ranked4.game.game_service.dto;

import java.util.UUID;

public record GifReactionEvent(
    UUID gameId,
    UUID playerId,
    String gifCode,
    String assetPath,
    long timestamp
) {}