package com.ranked4.game.game_service.dto;

import java.util.UUID;

public record PveGameResponse(
    UUID gameId,
    UUID playerId,
    int difficulty
) {}
