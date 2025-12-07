package com.ranked4.userprofile.userprofile_service.controller;

import java.util.List;
import java.util.Optional;
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

    @PutMapping("/me/avatar")
    public ResponseEntity<?> updateCurrentUserAvatar(
            @RequestHeader(value = "X-User-Id", required = true) String userIdHeader,
            @RequestBody UpdateAvatarRequestDTO request) {

        UUID userId;
        try {
            userId = UUID.fromString(userIdHeader);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid user ID format");
        }

        try {
            MyUserProfileDTO updated = userProfileService.updateAvatar(userId, request.avatarUrl());
            return ResponseEntity.ok(updated);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    record UpdateAvatarRequestDTO(String avatarUrl) {}

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

    @GetMapping("/adminUserList")
    public ResponseEntity<?> getAdminUserList(
            @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestHeader(value = "X-User-Roles") String userRoles
    ) {
        isAdmin(userRoles);

        Page<UserProfileDTO> profiles = userProfileService.getAdminUserList(pageable);

        if (!profiles.isEmpty()) {
            return ResponseEntity.ok(profiles);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No profiles found");
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

    @PostMapping("/{userId}/credit-gold")
    public ResponseEntity<?> creditGold(
            @RequestHeader(value = "X-User-Roles") String userRoles,
            @PathVariable String userId,
            @RequestParam("amount") int amount) {

        isAdmin(userRoles);

        UUID userUuid;
        try {
            userUuid = UUID.fromString(userId);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid user ID format");
        }

        try {
            userProfileService.creditGold(userUuid, amount);
            return ResponseEntity.ok().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<?> deleteUserProfile(
            @RequestHeader(value = "X-User-Roles") String userRoles,
            @PathVariable String userId) {

        isAdmin(userRoles);

        UUID userUuid;
        try {
            userUuid = UUID.fromString(userId);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid user ID format");
        }

        try {
            userProfileService.deleteUserProfile(userUuid);
            return ResponseEntity.ok().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @PostMapping("/{userId}/add-disc")
    public ResponseEntity<?> addDiscToUserByAdmin(
            @RequestHeader(value = "X-User-Roles") String userRoles,
            @PathVariable String userId,
            @RequestBody AddDiscToUserRequestDTO request) {

        isAdmin(userRoles);

        UUID userUuid;
        try {
            userUuid = UUID.fromString(userId);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid user ID format");
        }

        try {
            MyUserProfileDTO updated = userProfileService.addDiscToUserByAdmin(
                userUuid,
                request.itemCode(),
                request.equip()
            );
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @PostMapping("/{userId}/add-disc-with-result")
    public ResponseEntity<?> addDiscToUserWithResult(
            @RequestHeader(value = "X-User-Roles") String userRoles,
            @PathVariable String userId,
            @RequestBody AddDiscToUserRequestDTO request) {

        isAdmin(userRoles);

        UUID userUuid;
        try {
            userUuid = UUID.fromString(userId);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid user ID format");
        }

        try {
            var result = userProfileService.addDiscToUserWithResult(
                userUuid,
                request.itemCode(),
                request.equip()
            );
            return ResponseEntity.ok(new AddDiscResponseDTO(result.profile(), result.alreadyOwned()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    record AddDiscResponseDTO(MyUserProfileDTO profile, boolean alreadyOwned) {}

    @DeleteMapping("/{userId}/remove-disc/{itemCode}")
    public ResponseEntity<?> removeDiscFromUser(
            @RequestHeader(value = "X-User-Roles") String userRoles,
            @PathVariable String userId,
            @PathVariable String itemCode) {

        isAdmin(userRoles);

        UUID userUuid;
        try {
            userUuid = UUID.fromString(userId);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid user ID format");
        }

        try {
            MyUserProfileDTO updated = userProfileService.removeDiscFromUser(userUuid, itemCode);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
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