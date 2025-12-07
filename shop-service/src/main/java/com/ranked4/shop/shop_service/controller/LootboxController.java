package com.ranked4.shop.shop_service.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ranked4.shop.shop_service.DTO.CreateLootboxRequestDTO;
import com.ranked4.shop.shop_service.DTO.LootboxDTO;
import com.ranked4.shop.shop_service.DTO.LootboxOpeningResultDTO;
import com.ranked4.shop.shop_service.service.LootboxService;

@RestController
@RequestMapping("/api/shop/lootboxes")
public class LootboxController {

    private final LootboxService lootboxService;

    public LootboxController(LootboxService lootboxService) {
        this.lootboxService = lootboxService;
    }

    @GetMapping
    public ResponseEntity<Page<LootboxDTO>> getAllLootboxes(
            @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<LootboxDTO> lootboxes = lootboxService.getAllLootboxes(pageable);
        return ResponseEntity.ok(lootboxes);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getLootboxById(@PathVariable Long id) {
        try {
            LootboxDTO lootbox = lootboxService.getLootboxById(id);
            return ResponseEntity.ok(lootbox);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @PostMapping
    public ResponseEntity<?> createLootbox(
            @RequestHeader(value = "X-User-Roles") String userRoles,
            @RequestBody CreateLootboxRequestDTO request) {
        isAdmin(userRoles);

        try {
            LootboxDTO created = lootboxService.createLootbox(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateLootbox(
            @RequestHeader(value = "X-User-Roles") String userRoles,
            @PathVariable Long id,
            @RequestBody CreateLootboxRequestDTO request) {
        isAdmin(userRoles);

        try {
            LootboxDTO updated = lootboxService.updateLootbox(id, request);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteLootbox(
            @RequestHeader(value = "X-User-Roles") String userRoles,
            @PathVariable Long id) {
        isAdmin(userRoles);

        try {
            lootboxService.deleteLootbox(id);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @PostMapping("/{id}/open")
    public ResponseEntity<?> openLootbox(
            @RequestHeader(value = "X-User-Id", required = true) String userIdHeader,
            @PathVariable Long id) {

        UUID userId;
        try {
            userId = UUID.fromString(userIdHeader);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid user ID format");
        }

        try {
            LootboxOpeningResultDTO result = lootboxService.openLootbox(userId, id);
            return ResponseEntity.ok(result);
        } catch (IllegalStateException e) {
            if (e.getMessage().contains("Insufficient funds")) {
                return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED).body(e.getMessage());
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    private boolean isAdmin(String userRoles) {
        if (userRoles == null || userRoles.isEmpty()) {
            throw new AccessDeniedException("Access denied: requires the ROLE_ADMIN role.");
        }
        List<String> roles = List.of(userRoles.split(","));
        if (!roles.contains("ROLE_ADMIN")) {
            throw new AccessDeniedException("Access denied: requires the ROLE_ADMIN role.");
        }
        return true;
    }
}
