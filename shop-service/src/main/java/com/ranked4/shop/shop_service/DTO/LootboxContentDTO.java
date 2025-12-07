package com.ranked4.shop.shop_service.DTO;

import com.ranked4.shop.shop_service.model.LootboxContent;

public record LootboxContentDTO(
    Long id,
    String itemCode,
    String itemType,
    int weight,
    Integer goldAmount
) {
    public LootboxContentDTO(LootboxContent entity) {
        this(
            entity.getId(),
            entity.getItemCode(),
            entity.getItemType(),
            entity.getWeight(),
            entity.getGoldAmount()
        );
    }

    public static LootboxContentDTO fromEntity(LootboxContent entity) {
        if (entity == null) {
            return null;
        }
        return new LootboxContentDTO(entity);
    }
}
