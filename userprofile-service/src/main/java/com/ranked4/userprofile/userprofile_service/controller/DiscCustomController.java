package com.ranked4.userprofile.userprofile_service.controller;

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

import com.ranked4.userprofile.userprofile_service.dto.AddDiscToUserRequestDTO;
import com.ranked4.userprofile.userprofile_service.dto.DiscCustomizationDTO;
import com.ranked4.userprofile.userprofile_service.dto.MyUserProfileDTO;
import com.ranked4.userprofile.userprofile_service.service.DiscCustomService;
import com.ranked4.userprofile.userprofile_service.service.UserProfileService;

@RestController
@RequestMapping("/api/discs")
public class DiscCustomController {

    private final DiscCustomService discCustomService;
    private final UserProfileService userProfileService;

    public DiscCustomController(DiscCustomService discCustomService, UserProfileService userProfileService) {
        this.discCustomService = discCustomService;
        this.userProfileService = userProfileService;
    }

    @GetMapping()
    public ResponseEntity<Page<DiscCustomizationDTO>> getAllDiscCustomizations(
        @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.DESC) Pageable pageable,
        @RequestHeader(value = "X-User-Roles") String userRoles
    ) {
        Page<DiscCustomizationDTO> discCustomizations = discCustomService.getAllDiscCustomizations(pageable);
        return ResponseEntity.ok(discCustomizations);
    }

    @PostMapping
    public ResponseEntity<DiscCustomizationDTO> createDiscCustomization(
            @RequestHeader(value = "X-User-Roles") String userRoles,
            @RequestBody DiscCustomizationDTO request
    ) {
        List<String> roles = List.of(userRoles.split(","));
        if (!roles.contains("ROLE_ADMIN")) {
            throw new AccessDeniedException("Access denied: requires the ROLE_ADMIN role.");
        }

        DiscCustomizationDTO created = discCustomService.createDiscCustomization(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping("/purchase")
    public ResponseEntity<?> purchaseDiscCustomization(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestBody PurchaseDiscRequestDTO request
    ) {
        try {
            MyUserProfileDTO updated = discCustomService.purchaseDiscCustomization(userId, request.itemCode());
            return ResponseEntity.ok(new PurchaseDiscResponseDTO(
                true,
                "Purchase successful",
                updated.gold()
            ));
        } catch (IllegalStateException e) {
            if (e.getMessage().contains("Insufficient")) {
                return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED)
                    .body(new PurchaseDiscResponseDTO(false, e.getMessage(), null));
            } else if (e.getMessage().contains("already owns")) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new PurchaseDiscResponseDTO(false, e.getMessage(), null));
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new PurchaseDiscResponseDTO(false, e.getMessage(), null));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new PurchaseDiscResponseDTO(false, e.getMessage(), null));
        }
    }

    @PostMapping("/attach")
    public ResponseEntity<MyUserProfileDTO> addDiscToCurrentUser(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestBody AddDiscToUserRequestDTO request
    ) {
        MyUserProfileDTO updated = userProfileService.addDiscToUser(
                userId,
                request.itemCode(),
                request.equip()
        );
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/equip")
    public ResponseEntity<?> equipDisc(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestBody EquipDiscRequestDTO request
    ) {
        try {
            MyUserProfileDTO updated = userProfileService.equipDisc(userId, request.itemCode());
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
            } else if (e.getMessage().contains("does not own")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PostMapping("/unequip")
    public ResponseEntity<?> unequipDisc(@RequestHeader("X-User-Id") UUID userId) {
        try {
            MyUserProfileDTO updated = userProfileService.unequipDisc(userId);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    record PurchaseDiscRequestDTO(String itemCode) {}
    record PurchaseDiscResponseDTO(boolean success, String message, Integer newBalance) {}
    record EquipDiscRequestDTO(String itemCode) {}

    @PutMapping("/{itemCode}")
    public ResponseEntity<?> updateDiscCustomization(
            @RequestHeader(value = "X-User-Roles") String userRoles,
            @PathVariable String itemCode,
            @RequestBody DiscCustomizationDTO request
    ) {
        isAdmin(userRoles);

        try {
            DiscCustomizationDTO updated = discCustomService.updateDiscCustomization(itemCode, request);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @DeleteMapping("/{itemCode}")
    public ResponseEntity<?> deleteDiscCustomization(
            @RequestHeader(value = "X-User-Roles") String userRoles,
            @PathVariable String itemCode
    ) {
        isAdmin(userRoles);

        try {
            discCustomService.deleteDiscCustomization(itemCode);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    public boolean isAdmin(String userRoles) {
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
