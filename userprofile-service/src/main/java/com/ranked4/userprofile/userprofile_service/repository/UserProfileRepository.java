package com.ranked4.userprofile.userprofile_service.repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ranked4.userprofile.userprofile_service.model.UserProfile;

@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {

    Optional<UserProfile> findByUserId(UUID userId);

    boolean existsByUserId(UUID userId);

    Optional<UserProfile> findByDisplayName(String displayName);

    List<UserProfile> findTop10ByOrderByEloDesc();
    List<UserProfile> findAllByUserIdIn(List<UUID> userIds);
    List<UserProfile> findAllByUserIdIn(Set<UUID> userIds);
}