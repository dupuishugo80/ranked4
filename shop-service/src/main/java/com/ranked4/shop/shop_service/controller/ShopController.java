package com.ranked4.shop.shop_service.controller;

import org.springframework.security.access.AccessDeniedException;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ranked4.shop.shop_service.DTO.ProductDTO;
import com.ranked4.shop.shop_service.DTO.ProductRequestDTO;
import com.ranked4.shop.shop_service.model.Product;
import com.ranked4.shop.shop_service.model.Purchase;
import com.ranked4.shop.shop_service.service.ShopService;
import org.springframework.web.bind.annotation.RequestBody;


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

    @GetMapping("/products")
    public ResponseEntity<Page<ProductDTO>> getProducts(
            @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        
        Page<ProductDTO> products = shopService.getProducts(pageable);
        return ResponseEntity.ok(products);
    }

    @PostMapping("/products")
    public Product createProduct(@RequestHeader(value = "X-User-Roles") String userRoles, @RequestBody ProductRequestDTO productRequestDTO) {
        List<String> roles = List.of(userRoles.split(","));
        if (!roles.contains("ROLE_ADMIN")) {
            throw new AccessDeniedException("Access denied: requires the ROLE_ADMIN role.");
        }
        
        if(productRequestDTO.name() == null || productRequestDTO.imageUrl() == null) {
            throw new IllegalArgumentException("Product name and imageUrl cannot be null");
        }

        Product product = new Product();
        product.setName(productRequestDTO.name());
        product.setDescription(productRequestDTO.description());
        product.setImageUrl(productRequestDTO.imageUrl());
        product.setPrice(productRequestDTO.price());
        
        Product createdProduct = shopService.createProduct(product);
        return createdProduct;
    }
    
}