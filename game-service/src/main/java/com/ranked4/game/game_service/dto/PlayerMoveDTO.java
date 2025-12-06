package com.ranked4.game.game_service.dto;

import java.util.UUID;

public record PlayerMoveDTO(UUID gameId, UUID playerId, int column) {}
