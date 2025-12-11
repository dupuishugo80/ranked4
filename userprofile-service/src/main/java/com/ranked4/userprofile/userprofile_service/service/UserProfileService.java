package com.ranked4.userprofile.userprofile_service.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ranked4.userprofile.userprofile_service.dto.DiscCustomizationDTO;
import com.ranked4.userprofile.userprofile_service.dto.LeaderboardEntryDTO;
import com.ranked4.userprofile.userprofile_service.dto.MyUserProfileDTO;
import com.ranked4.userprofile.userprofile_service.dto.UserProfileDTO;
import com.ranked4.userprofile.userprofile_service.model.DiscCustomization;
import com.ranked4.userprofile.userprofile_service.model.UserProfile;
import com.ranked4.userprofile.userprofile_service.repository.DiscCustomizationRepository;
import com.ranked4.userprofile.userprofile_service.repository.UserProfileRepository;

@Service
public class UserProfileService {

    private final UserProfileRepository userProfileRepository;
    private final DiscCustomizationRepository discCustomizationRepository;
    private final ImageValidationService imageValidationService;

     public UserProfileService(
            UserProfileRepository userProfileRepository,
            DiscCustomizationRepository discCustomizationRepository,
            ImageValidationService imageValidationService) {
        this.userProfileRepository = userProfileRepository;
        this.discCustomizationRepository = discCustomizationRepository;
        this.imageValidationService = imageValidationService;
    }

    @Transactional(readOnly = true)
    public Optional<UserProfileDTO> getUserProfileByUserId(UUID userId) {
        return userProfileRepository.findByUserId(userId)
                .map(UserProfileDTO::fromEntity);
    }

    @Transactional(readOnly = true)
    public Optional<MyUserProfileDTO> getMyUserProfileByUserId(UUID userId) {
        return userProfileRepository.findByUserId(userId)
                .map(MyUserProfileDTO::fromEntity);
    }

    @Transactional
    public UserProfileDTO createProfile(UUID userId, String displayName) {
        if (userProfileRepository.existsByUserId(userId)) {
            throw new IllegalArgumentException("User profile already exists for user ID: " + userId);
        }

        UserProfile newUserProfile = new UserProfile();
        newUserProfile.setUserId(userId);
        newUserProfile.setDisplayName(displayName);
        newUserProfile.setAvatarUrl("https://cdn.pixabay.com/photo/2015/10/05/22/37/blank-profile-picture-973460_1280.png");

        UserProfile savedProfile = userProfileRepository.save(newUserProfile);
        return UserProfileDTO.fromEntity(savedProfile);
    }

    @Transactional(readOnly = true)
    public List<LeaderboardEntryDTO> getLeaderboard() {
        List<UserProfile> topProfiles = userProfileRepository.findTop10ByOrderByEloDesc();

        return IntStream.range(0, topProfiles.size())
                .mapToObj(index -> {
                    UserProfile profile = topProfiles.get(index);
                    return new LeaderboardEntryDTO(
                        profile.getUserId(),
                        profile.getDisplayName(),
                        profile.getAvatarUrl(),
                        profile.getElo(),
                        profile.getWins(),
                        profile.getLosses(),
                        profile.getDraws(),
                        index + 1
                    );
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<UserProfileDTO> getAdminUserList(Pageable pageable) {
        return userProfileRepository.findAll(pageable).map(UserProfileDTO::fromEntity);
    }

    @Transactional(readOnly = true)
    public List<UserProfileDTO> getProfilesByUserIds(List<UUID> userIds) {
        return userProfileRepository.findAllByUserIdIn(userIds).stream()
                .map(UserProfileDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public boolean debitGold(UUID userId, int amountToDebit) {
        if (amountToDebit <= 0) {
            throw new IllegalArgumentException("Le montant à débiter doit être positif.");
        }

        UserProfile profile = userProfileRepository.findByUserId(userId)
            .orElseThrow(() -> new IllegalStateException("Profil non trouvé pour l'utilisateur: " + userId));

        if (profile.getGold() >= amountToDebit) {
            profile.setGold(profile.getGold() - amountToDebit);
            userProfileRepository.save(profile);
            return true;
        } else {
            return false;
        }
    }

    @Transactional
    public MyUserProfileDTO addDiscToUser(UUID userId, String itemCode, boolean equip) {
        UserProfile profile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("User profile not found"));

        DiscCustomization disc = discCustomizationRepository.findByItemCode(itemCode)
                .orElseThrow(() -> new RuntimeException("DiscCustomization not found"));

        if (!profile.getOwnedDiscs().contains(disc)) {
            profile.getOwnedDiscs().add(disc);
        }

        if (equip) {
            profile.setEquippedDisc(disc);
        }

        UserProfile saved = userProfileRepository.save(profile);
        return MyUserProfileDTO.fromEntity(saved);
    }

    @Transactional
    public AddDiscResult addDiscToUserWithResult(UUID userId, String itemCode, boolean equip) {
        UserProfile profile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("User profile not found"));

        DiscCustomization disc = discCustomizationRepository.findByItemCode(itemCode)
                .orElseThrow(() -> new RuntimeException("DiscCustomization not found"));

        boolean alreadyOwned = profile.getOwnedDiscs().contains(disc);

        if (!alreadyOwned) {
            profile.getOwnedDiscs().add(disc);
        }

        if (equip && !alreadyOwned) {
            profile.setEquippedDisc(disc);
        }

        UserProfile saved = userProfileRepository.save(profile);
        return new AddDiscResult(MyUserProfileDTO.fromEntity(saved), alreadyOwned);
    }

    public record AddDiscResult(MyUserProfileDTO profile, boolean alreadyOwned) {}

    @Transactional
    public void creditGold(UUID userId, int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount to credit must be positive.");
        }

        UserProfile profile = userProfileRepository.findByUserId(userId)
            .orElseThrow(() -> new IllegalStateException("Profile not found for user: " + userId));

        profile.setGold(profile.getGold() + amount);
        userProfileRepository.save(profile);
    }

    @Transactional(readOnly = true)
    public boolean isDailyFreeAvailable(UUID userId) {
        UserProfile profile = userProfileRepository.findByUserId(userId)
            .orElseThrow(() -> new IllegalStateException("Profile not found for user: " + userId));

        LocalDate lastOpened = profile.getLastDailyFreeOpenedAt();
        LocalDate today = LocalDate.now();

        return lastOpened == null || !lastOpened.equals(today);
    }

    @Transactional
    public void updateLastDailyFreeOpened(UUID userId) {
        UserProfile profile = userProfileRepository.findByUserId(userId)
            .orElseThrow(() -> new IllegalStateException("Profile not found for user: " + userId));

        profile.setLastDailyFreeOpenedAt(LocalDate.now());
        userProfileRepository.save(profile);
    }

    @Transactional
    public void deleteUserProfile(UUID userId) {
        UserProfile profile = userProfileRepository.findByUserId(userId)
            .orElseThrow(() -> new IllegalStateException("Profile not found for user: " + userId));

        userProfileRepository.delete(profile);
    }

    @Transactional
    public MyUserProfileDTO addDiscToUserByAdmin(UUID userId, String itemCode, boolean equip) {
        return addDiscToUser(userId, itemCode, equip);
    }

    @Transactional
    public MyUserProfileDTO removeDiscFromUser(UUID userId, String itemCode) {
        UserProfile profile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("User profile not found"));

        DiscCustomization disc = discCustomizationRepository.findByItemCode(itemCode)
                .orElseThrow(() -> new RuntimeException("DiscCustomization not found"));

        if (profile.getOwnedDiscs().contains(disc)) {
            profile.getOwnedDiscs().remove(disc);

            // If this was the equipped disc, unequip it
            if (profile.getEquippedDisc() != null && profile.getEquippedDisc().equals(disc)) {
                profile.setEquippedDisc(null);
            }
        }

        UserProfile saved = userProfileRepository.save(profile);
        return MyUserProfileDTO.fromEntity(saved);
    }

    @Transactional
    public MyUserProfileDTO updateAvatar(UUID userId, String avatarUrl) {
        UserProfile profile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalStateException("Profile not found for user: " + userId));

        if (avatarUrl == null || avatarUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("Avatar URL cannot be empty");
        }

        ImageValidationService.ValidationResult validationResult =
            imageValidationService.validateImageUrl(avatarUrl);

        if (!validationResult.isValid()) {
            throw new IllegalArgumentException(validationResult.message());
        }

        profile.setAvatarUrl(avatarUrl);
        UserProfile saved = userProfileRepository.save(profile);
        return MyUserProfileDTO.fromEntity(saved);
    }

    @Transactional
    public MyUserProfileDTO equipDisc(UUID userId, String itemCode) {
        UserProfile profile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("User profile not found"));

        DiscCustomization disc = discCustomizationRepository.findByItemCode(itemCode)
                .orElseThrow(() -> new RuntimeException("DiscCustomization not found"));

        if (!profile.getOwnedDiscs().contains(disc)) {
            throw new IllegalStateException("User does not own this disc");
        }

        profile.setEquippedDisc(disc);
        UserProfile saved = userProfileRepository.save(profile);
        return MyUserProfileDTO.fromEntity(saved);
    }

    @Transactional
    public MyUserProfileDTO unequipDisc(UUID userId) {
        UserProfile profile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("User profile not found"));

        profile.setEquippedDisc(null);
        UserProfile saved = userProfileRepository.save(profile);
        return MyUserProfileDTO.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<UserIdNamePair> getDisplayNamesByUserIds(Set<UUID> userIds) {
        List<UserProfile> profiles = userProfileRepository.findAllByUserIdIn(userIds);

        return profiles.stream()
            .map(profile -> new UserIdNamePair(
                profile.getUserId(),
                profile.getDisplayName()
            ))
            .toList();
    }

    public record UserIdNamePair(UUID userId, String displayName) {
    }
}