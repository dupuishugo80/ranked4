package com.ranked4.userprofile.userprofile_service.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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

     public UserProfileService(UserProfileRepository userProfileRepository, DiscCustomizationRepository discCustomizationRepository) {
        this.userProfileRepository = userProfileRepository;
        this.discCustomizationRepository = discCustomizationRepository;
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
                    LeaderboardEntryDTO dto = new LeaderboardEntryDTO(profile);
                    dto.setRank(index + 1);
                    return dto;
                })
                .collect(Collectors.toList());
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
    public DiscCustomizationDTO createDiscCustomization(DiscCustomizationDTO dto) {
        discCustomizationRepository.findByItemCode(dto.getItemCode())
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("DiscCustomization with this itemCode already exists");
                });

        DiscCustomization entity = new DiscCustomization();
        entity.setItemCode(dto.getItemCode());
        entity.setDisplayName(dto.getDisplayName());
        entity.setType(dto.getType());
        entity.setValue(dto.getValue());

        DiscCustomization saved = discCustomizationRepository.save(entity);
        return new DiscCustomizationDTO(saved);
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
}