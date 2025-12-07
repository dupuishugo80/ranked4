package com.ranked4.shop.shop_service.model;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "lootbox_openings")
public class LootboxOpening {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private Long lootboxId;

    @Column(nullable = false, length = 50)
    private String rewardItemCode;

    @Column(nullable = false, length = 20)
    private String rewardItemType;

    @Column(nullable = true)
    private Integer rewardGoldAmount;

    @Column(nullable = false)
    private Instant openedAt;

    public LootboxOpening() {
        this.openedAt = Instant.now();
    }

    public LootboxOpening(UUID userId, Long lootboxId, String rewardItemCode, String rewardItemType, Integer rewardGoldAmount) {
        this.userId = userId;
        this.lootboxId = lootboxId;
        this.rewardItemCode = rewardItemCode;
        this.rewardItemType = rewardItemType;
        this.rewardGoldAmount = rewardGoldAmount;
        this.openedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public Long getLootboxId() {
        return lootboxId;
    }

    public void setLootboxId(Long lootboxId) {
        this.lootboxId = lootboxId;
    }

    public String getRewardItemCode() {
        return rewardItemCode;
    }

    public void setRewardItemCode(String rewardItemCode) {
        this.rewardItemCode = rewardItemCode;
    }

    public String getRewardItemType() {
        return rewardItemType;
    }

    public void setRewardItemType(String rewardItemType) {
        this.rewardItemType = rewardItemType;
    }

    public Integer getRewardGoldAmount() {
        return rewardGoldAmount;
    }

    public void setRewardGoldAmount(Integer rewardGoldAmount) {
        this.rewardGoldAmount = rewardGoldAmount;
    }

    public Instant getOpenedAt() {
        return openedAt;
    }

    public void setOpenedAt(Instant openedAt) {
        this.openedAt = openedAt;
    }
}
