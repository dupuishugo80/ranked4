package com.ranked4.shop.shop_service.service;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.ranked4.shop.shop_service.model.Product;
import com.ranked4.shop.shop_service.model.Purchase;
import com.ranked4.shop.shop_service.repository.ProductRepository;
import com.ranked4.shop.shop_service.repository.PurchaseRepository;

@Service
public class ShopService {

    private final ProductRepository productRepository;
    private final PurchaseRepository purchaseRepository;
    private final WebClient userProfileClient;

    public ShopService(ProductRepository productRepository, 
                        PurchaseRepository purchaseRepository,
                        WebClient userProfileClient) {
            this.productRepository = productRepository;
            this.purchaseRepository = purchaseRepository;
            this.userProfileClient = userProfileClient;
    }

    @Transactional
    public Purchase buyProduct(UUID userId, Long productId) {
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));

        int price = product.getPrice();

        try {
            userProfileClient.post()
                .uri(uriBuilder -> uriBuilder
                    .path("/api/profiles/debit-gold")
                    .queryParam("amount", price)
                    .build())
                .header("X-User-Id", userId.toString())
                .retrieve()
                .toBodilessEntity()
                .block();

        } catch (WebClientResponseException e) {
            if (e.getStatusCode() == HttpStatus.PAYMENT_REQUIRED) {
                throw new IllegalStateException("Payment failed: Insufficient funds.");
            } else if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new IllegalStateException("User not found during payment.");
            } else {
                throw new RuntimeException("Communication error with the profile service.", e);
            }
        }

        Purchase newPurchase = new Purchase();
        newPurchase.setUserId(userId);
        newPurchase.setProductId(productId);
        newPurchase.setPriceAtPurchase(price);
        
        return purchaseRepository.save(newPurchase);
    }
}