package com.ranked4.shop.shop_service.DTO;

import com.ranked4.shop.shop_service.model.Product;

public class ProductDTO {

    private Long id;
    private String name;
    private String description;
    private int price;

    public ProductDTO(Product entity) {
        this.id = entity.getId();
        this.name = entity.getName();
        this.description = entity.getDescription();
        this.price = entity.getPrice();
    }

    public static ProductDTO fromEntity(Product entity) {
        if (entity == null) {
            return null;
        }
        return new ProductDTO(entity);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getPrice() {
        return price;
    }

    public void setPrice(int price) {
        this.price = price;
    }
}