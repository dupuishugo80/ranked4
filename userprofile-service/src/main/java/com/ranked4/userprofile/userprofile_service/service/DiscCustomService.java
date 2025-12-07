package com.ranked4.userprofile.userprofile_service.service;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestHeader;

import com.ranked4.userprofile.userprofile_service.dto.DiscCustomizationDTO;
import com.ranked4.userprofile.userprofile_service.dto.MyUserProfileDTO;
import com.ranked4.userprofile.userprofile_service.model.DiscCustomization;
import com.ranked4.userprofile.userprofile_service.model.UserProfile;
import com.ranked4.userprofile.userprofile_service.repository.DiscCustomizationRepository;
import com.ranked4.userprofile.userprofile_service.repository.UserProfileRepository;

@Service
public class DiscCustomService {

    private final DiscCustomizationRepository discCustomizationRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserProfileService userProfileService;

    public DiscCustomService(DiscCustomizationRepository discCustomizationRepository,
            UserProfileRepository userProfileRepository,
            UserProfileService userProfileService) {
        this.discCustomizationRepository = discCustomizationRepository;
        this.userProfileRepository = userProfileRepository;
        this.userProfileService = userProfileService;
    }

    public Page<DiscCustomizationDTO> getAllDiscCustomizations(Pageable pageable) {
        Page<DiscCustomization> entities = discCustomizationRepository.findAll(pageable);
        return entities.map(DiscCustomizationDTO::new);
    }

    @Transactional
    public DiscCustomizationDTO createDiscCustomization(DiscCustomizationDTO dto) {
        discCustomizationRepository.findByItemCode(dto.itemCode())
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("DiscCustomization with this itemCode already exists");
                });

        DiscCustomization entity = new DiscCustomization();
        entity.setItemCode(dto.itemCode());
        entity.setDisplayName(dto.displayName());
        entity.setType(dto.type());
        entity.setValue(dto.value());
        entity.setPrice(dto.price());
        entity.setAvailableForPurchase(dto.availableForPurchase() != null ? dto.availableForPurchase() : true);

        DiscCustomization saved = discCustomizationRepository.save(entity);
        return new DiscCustomizationDTO(saved);
    }

    @Transactional
    public DiscCustomizationDTO updateDiscCustomization(String itemCode, DiscCustomizationDTO dto) {
        DiscCustomization entity = discCustomizationRepository.findByItemCode(itemCode)
                .orElseThrow(
                        () -> new IllegalArgumentException("DiscCustomization not found with itemCode: " + itemCode));

        entity.setDisplayName(dto.displayName());
        entity.setType(dto.type());
        entity.setValue(dto.value());
        entity.setPrice(dto.price());
        entity.setAvailableForPurchase(
                dto.availableForPurchase() != null ? dto.availableForPurchase() : entity.getAvailableForPurchase());

        DiscCustomization saved = discCustomizationRepository.save(entity);
        return new DiscCustomizationDTO(saved);
    }

    @Transactional
    public void deleteDiscCustomization(String itemCode) {
        DiscCustomization entity = discCustomizationRepository.findByItemCode(itemCode)
                .orElseThrow(
                        () -> new IllegalArgumentException("DiscCustomization not found with itemCode: " + itemCode));

        List<UserProfile> usersWithDisc = userProfileRepository.findAll().stream()
                .filter(user -> user.getOwnedDiscs().contains(entity))
                .toList();

        for (UserProfile user : usersWithDisc) {
            if (user.getEquippedDisc() != null && user.getEquippedDisc().equals(entity)) {
                user.setEquippedDisc(null);
            }
            user.getOwnedDiscs().remove(entity);
            userProfileRepository.save(user);
        }

        discCustomizationRepository.delete(entity);
    }

    @Transactional
    public MyUserProfileDTO purchaseDiscCustomization(UUID userId, String itemCode) {
        DiscCustomization disc = discCustomizationRepository.findByItemCode(itemCode)
                .orElseThrow(
                        () -> new IllegalArgumentException("Disc customization not found with itemCode: " + itemCode));

        if (disc.getPrice() == null || disc.getPrice() <= 0) {
            throw new IllegalStateException("This disc is not available for purchase");
        }

        UserProfile profile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("User profile not found"));

        if (profile.getOwnedDiscs().contains(disc)) {
            throw new IllegalStateException("User already owns this disc");
        }

        if (profile.getGold() < disc.getPrice()) {
            throw new IllegalStateException(
                    "Insufficient gold. Required: " + disc.getPrice() + ", Available: " + profile.getGold());
        }

        boolean debited = userProfileService.debitGold(userId, disc.getPrice());
        if (!debited) {
            throw new IllegalStateException("Failed to debit gold");
        }

        return userProfileService.addDiscToUser(userId, itemCode, false);
    }

}
