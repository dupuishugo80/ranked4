package com.ranked4.userprofile.userprofile_service.controller;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ranked4.userprofile.userprofile_service.dto.AddDiscToUserRequestDTO;
import com.ranked4.userprofile.userprofile_service.dto.DiscCustomizationDTO;
import com.ranked4.userprofile.userprofile_service.dto.LeaderboardEntryDTO;
import com.ranked4.userprofile.userprofile_service.dto.MyUserProfileDTO;
import com.ranked4.userprofile.userprofile_service.dto.UserProfileDTO;
import com.ranked4.userprofile.userprofile_service.service.UserProfileService;

@RestController
@RequestMapping("/api/profiles")
public class UserProfileController {

    private final UserProfileService userProfileService;

    public UserProfileController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUserProfile(@RequestHeader(value = "X-User-Id", required = true) String userIdHeader) {
        UUID userId;
        try {
            userId = UUID.fromString(userIdHeader);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid user ID format");
        }

        Optional<MyUserProfileDTO> profileOpt = userProfileService.getMyUserProfileByUserId(userId);

        if (profileOpt.isPresent()) {
            return ResponseEntity.ok(profileOpt.get());
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Profile not found");
        }
    }

    @GetMapping("/{userId}")
    public ResponseEntity<?> getUserProfileById(@PathVariable String userId) {
        UUID userUuid;
        try {
            userUuid = UUID.fromString(userId);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid user ID format");
        }

        Optional<UserProfileDTO> profileOpt = userProfileService.getUserProfileByUserId(userUuid);

        if (profileOpt.isPresent()) {
            return ResponseEntity.ok(profileOpt.get());
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Profile not found");
        }
    }

    @GetMapping("/leaderboard")
    public ResponseEntity<List<LeaderboardEntryDTO>> getLeaderboard() {
        List<LeaderboardEntryDTO> leaderboard = userProfileService.getLeaderboard();
        return ResponseEntity.ok(leaderboard);
    }

    @PostMapping("/fullprofilesbyids")
    public ResponseEntity<List<UserProfileDTO>> getProfilesByIds(@RequestBody List<UUID> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        List<UserProfileDTO> profiles = userProfileService.getProfilesByUserIds(userIds);
        return ResponseEntity.ok(profiles);
    }

    @PostMapping("/debit-gold")
    public ResponseEntity<?> debitGold(
            @RequestHeader(value = "X-User-Id", required = true) String userIdHeader,
            @RequestParam("amount") int amount) {
        
        UUID userId;
        try {
            userId = UUID.fromString(userIdHeader);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid user ID format");
        }

        try {
            boolean success = userProfileService.debitGold(userId, amount);
            
            if (success) {
                return ResponseEntity.ok().build(); 
            } else {
                return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED).body("Insufficient funds");
            }
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (IllegalArgumentException e) {
             return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PostMapping("/discs")
    public ResponseEntity<DiscCustomizationDTO> createDiscCustomization(
            @RequestHeader(value = "X-User-Roles") String userRoles,
            @RequestBody DiscCustomizationDTO request
    ) {
        List<String> roles = List.of(userRoles.split(","));
        if (!roles.contains("ROLE_ADMIN")) {
            throw new AccessDeniedException("Access denied: requires the ROLE_ADMIN role.");
        }

        DiscCustomizationDTO created = userProfileService.createDiscCustomization(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping("/discs/attach")
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