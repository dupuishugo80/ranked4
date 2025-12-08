package com.ranked4.game.game_service.dto;

public record AiMoveRequest(
    String grid,
    int difficulty,
    int aiPlayerId
) {}
