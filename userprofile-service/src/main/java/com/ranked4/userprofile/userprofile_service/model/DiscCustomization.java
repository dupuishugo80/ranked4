package com.ranked4.userprofile.userprofile_service.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "disc_customizations")
public class DiscCustomization {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String itemCode;

    @Column(nullable = false)
    private String displayName;

    @Column(nullable = false, length = 10) // "color", "image"
    private String type;

    @Column(nullable = false)
    private String value;

    @Column(nullable = true)
    private Integer price;

    @Column(nullable = false)
    private Boolean availableForPurchase = true;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getItemCode() {
        return itemCode;
    }

    public void setItemCode(String itemCode) {
        this.itemCode = itemCode;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public Integer getPrice() {
        return price;
    }

    public void setPrice(Integer price) {
        this.price = price;
    }

    public Boolean getAvailableForPurchase() {
        return availableForPurchase;
    }

    public void setAvailableForPurchase(Boolean availableForPurchase) {
        this.availableForPurchase = availableForPurchase;
    }
}
