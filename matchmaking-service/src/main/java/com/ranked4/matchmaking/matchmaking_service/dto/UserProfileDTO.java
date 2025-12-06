package com.ranked4.matchmaking.matchmaking_service.dto;

import java.util.UUID;

public record UserProfileDTO(UUID userId, String displayName, int elo) {}
