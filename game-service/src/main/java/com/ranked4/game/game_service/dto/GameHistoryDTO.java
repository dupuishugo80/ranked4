package com.ranked4.game.game_service.dto;

import java.time.Instant;
import java.util.UUID;

import com.ranked4.game.game_service.model.Disc;

public record GameHistoryDTO(
    UUID gameId,
    UUID playerOneId,
    String playerOneName,
    UUID playerTwoId,
    String playerTwoName,
    Disc winner,
    Instant createdAt
) {}