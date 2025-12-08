package com.ranked4.shop.shop_service.DTO;

import java.util.List;

public record CreateLootboxRequestDTO(
    String name,
    String description,
    String imageUrl,
    int price,
    boolean dailyFree,
    List<CreateLootboxContentDTO> contents
) {
    public record CreateLootboxContentDTO(
        String itemCode,
        String itemType,
        int weight,
        Integer goldAmount
    ) {}
}
