package com.ranked4.game.game_service.dto;

public record UpdateGifRequestDTO(
    String code,
    String assetPath,
    Boolean active
) {}
