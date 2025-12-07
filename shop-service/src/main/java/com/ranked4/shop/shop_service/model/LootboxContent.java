package com.ranked4.shop.shop_service.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "lootbox_contents")
public class LootboxContent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "lootbox_id", nullable = false)
    private Lootbox lootbox;

    @Column(nullable = false, length = 50)
    private String itemCode;

    @Column(nullable = false, length = 20)
    private String itemType; // "DISC" or "GOLD"

    @Column(nullable = false)
    private int weight;

    @Column(nullable = true)
    private Integer goldAmount;

    public LootboxContent() {}

    public LootboxContent(Lootbox lootbox, String itemCode, String itemType, int weight, Integer goldAmount) {
        this.lootbox = lootbox;
        this.itemCode = itemCode;
        this.itemType = itemType;
        this.weight = weight;
        this.goldAmount = goldAmount;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Lootbox getLootbox() {
        return lootbox;
    }

    public void setLootbox(Lootbox lootbox) {
        this.lootbox = lootbox;
    }

    public String getItemCode() {
        return itemCode;
    }

    public void setItemCode(String itemCode) {
        this.itemCode = itemCode;
    }

    public String getItemType() {
        return itemType;
    }

    public void setItemType(String itemType) {
        this.itemType = itemType;
    }

    public int getWeight() {
        return weight;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }

    public Integer getGoldAmount() {
        return goldAmount;
    }

    public void setGoldAmount(Integer goldAmount) {
        this.goldAmount = goldAmount;
    }
}
