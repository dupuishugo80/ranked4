package com.ranked4.game.game_service.dto;

import java.util.UUID;

public record GifReactionMessage(UUID gameId, UUID playerId, String gifCode) {}