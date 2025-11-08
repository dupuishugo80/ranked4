package com.ranked4.userprofile.userprofile_service.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ranked4.userprofile.userprofile_service.dto.LeaderboardEntryDTO;
import com.ranked4.userprofile.userprofile_service.dto.UserProfileDTO;
import com.ranked4.userprofile.userprofile_service.model.UserProfile;
import com.ranked4.userprofile.userprofile_service.repository.UserProfileRepository;

@Service
public class UserProfileService {

    private final UserProfileRepository userProfileRepository;

    public UserProfileService(UserProfileRepository userProfileRepository) {
        this.userProfileRepository = userProfileRepository;
    }

    @Transactional(readOnly = true)
    public Optional<UserProfileDTO> getUserProfileByUserId(UUID userId) {
        return userProfileRepository.findByUserId(userId)
                .map(UserProfileDTO::fromEntity);
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
        List<UserProfile> topProfiles = userProfileRepository.findTop5ByOrderByEloDesc();

        return IntStream.range(0, topProfiles.size())
                .mapToObj(index -> {
                    UserProfile profile = topProfiles.get(index);
                    LeaderboardEntryDTO dto = new LeaderboardEntryDTO(profile);
                    dto.setRank(index + 1);
                    return dto;
                })
                .collect(Collectors.toList());
    }
}