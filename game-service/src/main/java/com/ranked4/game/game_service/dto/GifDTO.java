package com.ranked4.game.game_service.dto;

public record GifDTO(Long id, String code, String assetPath, Boolean active) {
    public GifDTO(Long id, String code, String assetPath) {
        this(id, code, assetPath, true);
    }
}
