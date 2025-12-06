package com.ranked4.matchmaking.matchmaking_service.dto;

import java.io.Serializable;
import java.util.UUID;

public record MatchmakingRequest(
    UUID userId,
    String displayName,
    int elo
) implements Serializable {}