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
@Table(name = "purchases")
public class Purchase {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private Long productId;

    @Column(nullable = false)
    private int priceAtPurchase;

    @Column(nullable = false)
    private Instant purchaseAt = Instant.now();

    public Purchase() {}

    public Purchase(UUID userId, Long productId, int priceAtPurchase, Instant purchaseAt) {
        this.userId = userId;
        this.productId = productId;
        this.priceAtPurchase = priceAtPurchase;
        this.purchaseAt = purchaseAt;
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

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public int getPriceAtPurchase() {
        return priceAtPurchase;
    }

    public void setPriceAtPurchase(int priceAtPurchase) {
        this.priceAtPurchase = priceAtPurchase;
    }

    public Instant getPurchaseAt() {
        return purchaseAt;
    }

    public void setPurchaseAt(Instant purchaseAt) {
        this.purchaseAt = purchaseAt;
    }
}
    