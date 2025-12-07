package com.ranked4.game.game_service.dto;

public record CreateGifRequestDTO(
    String code,
    String assetPath,
    Boolean active
) {
    public CreateGifRequestDTO {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Code cannot be null or blank");
        }
        if (assetPath == null || assetPath.isBlank()) {
            throw new IllegalArgumentException("AssetPath cannot be null or blank");
        }
        if (active == null) {
            active = true;
        }
    }
}
