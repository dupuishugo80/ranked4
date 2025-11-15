package com.ranked4.userprofile.userprofile_service.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.PostMapping;
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

    @PostMapping("/attach")
    public ResponseEntity<MyUserProfileDTO> addDiscToCurrentUser(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestBody AddDiscToUserRequestDTO request
    ) {
        MyUserProfileDTO updated = userProfileService.addDiscToUser(
                userId,
                request.getItemCode(),
                request.isEquip()
        );
        return ResponseEntity.ok(updated);
    }
    
}
