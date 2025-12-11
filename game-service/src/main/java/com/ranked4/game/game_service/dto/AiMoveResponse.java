package com.ranked4.game.game_service.dto;

public record AiMoveResponse(
    int column,
    boolean isWinningMove
) {}
