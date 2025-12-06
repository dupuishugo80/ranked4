package com.ranked4.shop.shop_service.DTO;

import com.ranked4.shop.shop_service.model.Product;

public record ProductDTO(
    Long id,
    String name,
    String description,
    int price
) {
    public ProductDTO(Product entity) {
        this(
            entity.getId(),
            entity.getName(),
            entity.getDescription(),
            entity.getPrice()
        );
    }

    public static ProductDTO fromEntity(Product entity) {
        if (entity == null) {
            return null;
        }
        return new ProductDTO(entity);
    }
}