package com.ranked4.userprofile.userprofile_service.service;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

        UserProfile savedProfile = userProfileRepository.save(newUserProfile);
        return UserProfileDTO.fromEntity(savedProfile);
    }

}