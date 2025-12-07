package com.ranked4.shop.shop_service.DTO;

import java.util.List;
import java.util.stream.Collectors;

import com.ranked4.shop.shop_service.model.Lootbox;

public record LootboxDTO(
    Long id,
    String name,
    String description,
    String imageUrl,
    int price,
    List<LootboxContentDTO> contents
) {
    public LootboxDTO(Lootbox entity) {
        this(
            entity.getId(),
            entity.getName(),
            entity.getDescription(),
            entity.getImageUrl(),
            entity.getPrice(),
            entity.getContents().stream()
                .map(LootboxContentDTO::new)
                .collect(Collectors.toList())
        );
    }

    public static LootboxDTO fromEntity(Lootbox entity) {
        if (entity == null) {
            return null;
        }
        return new LootboxDTO(entity);
    }
}
