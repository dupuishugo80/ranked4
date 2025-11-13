package com.ranked4.shop.shop_service.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ranked4.shop.shop_service.model.Purchase;
import com.ranked4.shop.shop_service.service.ShopService;

@RestController
@RequestMapping("/api/shop")
public class ShopController {

    private final ShopService shopService;

    public ShopController(ShopService shopService) {
        this.shopService = shopService;
    }

    @PostMapping("/buy/{productId}")
    public ResponseEntity<?> buyProduct(
            @RequestHeader(value = "X-User-Id", required = true) String userIdHeader,
            @PathVariable Long productId) {
        
        UUID userId = UUID.fromString(userIdHeader);
        Purchase purchase = shopService.buyProduct(userId, productId);
        return ResponseEntity.ok(purchase);
    }
}