package com.ranked4.shop.shop_service.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.ranked4.shop.shop_service.DTO.CreateLootboxRequestDTO;
import com.ranked4.shop.shop_service.DTO.LootboxDTO;
import com.ranked4.shop.shop_service.DTO.LootboxOpeningResultDTO;
import com.ranked4.shop.shop_service.DTO.RecentDropDTO;
import com.ranked4.shop.shop_service.model.Lootbox;
import com.ranked4.shop.shop_service.model.LootboxContent;
import com.ranked4.shop.shop_service.model.LootboxOpening;
import com.ranked4.shop.shop_service.repository.LootboxOpeningRepository;
import com.ranked4.shop.shop_service.repository.LootboxRepository;

@Service
public class LootboxService {

    private final LootboxRepository lootboxRepository;
    private final LootboxOpeningRepository lootboxOpeningRepository;
    private final WebClient userProfileClient;

    public LootboxService(
            LootboxRepository lootboxRepository,
            LootboxOpeningRepository lootboxOpeningRepository,
            WebClient userProfileClient) {
        this.lootboxRepository = lootboxRepository;
        this.lootboxOpeningRepository = lootboxOpeningRepository;
        this.userProfileClient = userProfileClient;
    }

    @Transactional(readOnly = true)
    public Page<LootboxDTO> getAllLootboxes(Pageable pageable) {
        List<Lootbox> allLootboxes = lootboxRepository.findAll();

        allLootboxes.sort((a, b) -> {
            if (a.isDailyFree() && !b.isDailyFree())
                return -1;
            if (!a.isDailyFree() && b.isDailyFree())
                return 1;
            return Long.compare(b.getId(), a.getId());
        });

        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), allLootboxes.size());
        List<Lootbox> pageContent = allLootboxes.subList(start, end);

        Page<Lootbox> lootboxPage = new org.springframework.data.domain.PageImpl<>(
                pageContent, pageable, allLootboxes.size());
        return lootboxPage.map(LootboxDTO::fromEntity);
    }

    @Transactional(readOnly = true)
    public LootboxDTO getLootboxById(Long id) {
        Lootbox lootbox = lootboxRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Lootbox not found: " + id));
        return LootboxDTO.fromEntity(lootbox);
    }

    @Transactional
    public LootboxDTO createLootbox(CreateLootboxRequestDTO request) {
        if (request.contents() == null || request.contents().isEmpty()) {
            throw new IllegalArgumentException("Lootbox must have at least one content item");
        }

        Lootbox lootbox = new Lootbox();
        lootbox.setName(request.name());
        lootbox.setDescription(request.description());
        lootbox.setImageUrl(request.imageUrl());
        lootbox.setPrice(request.price());
        lootbox.setDailyFree(request.dailyFree());

        List<LootboxContent> contents = new ArrayList<>();
        for (CreateLootboxRequestDTO.CreateLootboxContentDTO contentDTO : request.contents()) {
            LootboxContent content = new LootboxContent();
            content.setLootbox(lootbox);
            content.setItemCode(contentDTO.itemCode());
            content.setItemType(contentDTO.itemType());
            content.setWeight(contentDTO.weight());
            content.setGoldAmount(contentDTO.goldAmount());
            contents.add(content);
        }
        lootbox.setContents(contents);

        Lootbox saved = lootboxRepository.save(lootbox);
        return LootboxDTO.fromEntity(saved);
    }

    @Transactional
    public LootboxDTO updateLootbox(Long id, CreateLootboxRequestDTO request) {
        Lootbox lootbox = lootboxRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Lootbox not found: " + id));

        if (request.contents() == null || request.contents().isEmpty()) {
            throw new IllegalArgumentException("Lootbox must have at least one content item");
        }

        lootbox.setName(request.name());
        lootbox.setDescription(request.description());
        lootbox.setImageUrl(request.imageUrl());
        lootbox.setPrice(request.price());
        lootbox.setDailyFree(request.dailyFree());

        lootbox.getContents().clear();

        List<LootboxContent> newContents = new ArrayList<>();
        for (CreateLootboxRequestDTO.CreateLootboxContentDTO contentDTO : request.contents()) {
            LootboxContent content = new LootboxContent();
            content.setLootbox(lootbox);
            content.setItemCode(contentDTO.itemCode());
            content.setItemType(contentDTO.itemType());
            content.setWeight(contentDTO.weight());
            content.setGoldAmount(contentDTO.goldAmount());
            newContents.add(content);
        }
        lootbox.getContents().addAll(newContents);

        Lootbox saved = lootboxRepository.save(lootbox);
        return LootboxDTO.fromEntity(saved);
    }

    @Transactional
    public void deleteLootbox(Long id) {
        Lootbox lootbox = lootboxRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Lootbox not found: " + id));
        lootboxRepository.delete(lootbox);
    }

    @Transactional
    public LootboxOpeningResultDTO openLootbox(UUID userId, Long lootboxId) {
        Lootbox lootbox = lootboxRepository.findById(lootboxId)
                .orElseThrow(() -> new IllegalArgumentException("Lootbox not found: " + lootboxId));

        if (lootbox.isDailyFree()) {
            try {
                DailyFreeAvailabilityResponse response = userProfileClient.get()
                        .uri("/api/profiles/daily-free-available")
                        .header("X-User-Id", userId.toString())
                        .retrieve()
                        .bodyToMono(DailyFreeAvailabilityResponse.class)
                        .block();

                if (response == null || !response.available()) {
                    throw new IllegalStateException("Daily free lootbox already opened today.");
                }

                userProfileClient.post()
                        .uri("/api/profiles/update-daily-free")
                        .header("X-User-Id", userId.toString())
                        .retrieve()
                        .toBodilessEntity()
                        .block();

            } catch (WebClientResponseException e) {
                if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                    throw new IllegalStateException("User not found.");
                } else {
                    throw new RuntimeException("Communication error with the profile service.", e);
                }
            }
        } else {
            int price = lootbox.getPrice();

            try {
                userProfileClient.post()
                        .uri(uriBuilder -> uriBuilder
                                .path("/api/profiles/debit-gold")
                                .queryParam("amount", price)
                                .build())
                        .header("X-User-Id", userId.toString())
                        .retrieve()
                        .toBodilessEntity()
                        .block();

            } catch (WebClientResponseException e) {
                if (e.getStatusCode() == HttpStatus.PAYMENT_REQUIRED) {
                    throw new IllegalStateException("Payment failed: Insufficient funds.");
                } else if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                    throw new IllegalStateException("User not found during payment.");
                } else {
                    throw new RuntimeException("Communication error with the profile service.", e);
                }
            }
        }

        LootboxContent selectedReward = selectRandomReward(lootbox.getContents());

        String actualRewardItemCode = selectedReward.getItemCode();
        String actualRewardType = selectedReward.getItemType();
        Integer actualGoldAmount = selectedReward.getGoldAmount();
        String displayMessage = null;

        if ("DISC".equals(selectedReward.getItemType())) {
            try {
                AddDiscWithResultResponse response = userProfileClient.post()
                        .uri("/api/profiles/" + userId.toString() + "/add-disc-with-result")
                        .header("X-User-Roles", "ROLE_ADMIN")
                        .bodyValue(new AddDiscRequest(selectedReward.getItemCode(), true))
                        .retrieve()
                        .bodyToMono(AddDiscWithResultResponse.class)
                        .block();

                if (response != null && response.alreadyOwned()) {
                    int compensationGold = lootbox.getPrice() / 2;
                    try {
                        userProfileClient.post()
                                .uri(uriBuilder -> uriBuilder
                                        .path("/api/profiles/" + userId.toString() + "/credit-gold")
                                        .queryParam("amount", compensationGold)
                                        .build())
                                .header("X-User-Roles", "ROLE_ADMIN")
                                .retrieve()
                                .toBodilessEntity()
                                .block();
                    } catch (WebClientResponseException e) {
                        System.err.println("Failed to credit gold. Status: " + e.getStatusCode() + ", Body: "
                                + e.getResponseBodyAsString());
                        throw new RuntimeException("Failed to credit compensation gold: " + e.getMessage(), e);
                    } catch (Exception e) {
                        System.err.println("Unexpected error crediting gold: " + e.getMessage());
                        throw new RuntimeException("Failed to credit compensation gold: " + e.getMessage(), e);
                    }

                    actualRewardType = "GOLD";
                    actualGoldAmount = compensationGold;
                    displayMessage = "You already own this skin! You receive " + compensationGold
                            + " gold as compensation.";
                }
            } catch (WebClientResponseException e) {
                throw new RuntimeException("Failed to add disc to user inventory.", e);
            }
        } else if ("GOLD".equals(selectedReward.getItemType())) {
            if (selectedReward.getGoldAmount() != null && selectedReward.getGoldAmount() > 0) {
                try {
                    userProfileClient.post()
                            .uri(uriBuilder -> uriBuilder
                                    .path("/api/profiles/" + userId.toString() + "/credit-gold")
                                    .queryParam("amount", selectedReward.getGoldAmount())
                                    .build())
                            .header("X-User-Roles", "ROLE_ADMIN")
                            .retrieve()
                            .toBodilessEntity()
                            .block();
                } catch (WebClientResponseException e) {
                    throw new RuntimeException("Failed to credit gold to user.", e);
                }
            }
        }

        LootboxOpening opening = new LootboxOpening(
                userId,
                lootboxId,
                actualRewardItemCode,
                actualRewardType,
                actualGoldAmount);
        LootboxOpening savedOpening = lootboxOpeningRepository.save(opening);

        return new LootboxOpeningResultDTO(
                savedOpening.getId(),
                actualRewardItemCode,
                actualRewardType,
                actualGoldAmount,
                displayMessage);
    }

    private LootboxContent selectRandomReward(List<LootboxContent> contents) {
        if (contents == null || contents.isEmpty()) {
            throw new IllegalStateException("Lootbox has no contents");
        }

        int totalWeight = contents.stream().mapToInt(LootboxContent::getWeight).sum();

        if (totalWeight <= 0) {
            throw new IllegalStateException("Total weight must be greater than 0");
        }

        int random = ThreadLocalRandom.current().nextInt(totalWeight);
        int cumulative = 0;

        for (LootboxContent content : contents) {
            cumulative += content.getWeight();
            if (random < cumulative) {
                return content;
            }
        }

        return contents.get(contents.size() - 1);
    }

    @Transactional(readOnly = true)
    public List<RecentDropDTO> getRecentDrops(Long lootboxId) {
        Pageable limit5 = PageRequest.of(0, 5);
        List<LootboxOpening> recentOpenings = lootboxOpeningRepository
                .findTop5ByLootboxIdOrderByOpenedAtDesc(lootboxId, limit5);

        if (recentOpenings.isEmpty()) {
            return Collections.emptyList();
        }

        Set<UUID> userIds = recentOpenings.stream()
                .map(LootboxOpening::getUserId)
                .collect(Collectors.toSet());

        Map<UUID, String> displayNameMap = fetchDisplayNames(userIds);

        return recentOpenings.stream()
                .map(opening -> new RecentDropDTO(
                        displayNameMap.getOrDefault(opening.getUserId(), "Unknown Player"),
                        opening.getUserId().toString(),
                        opening.getRewardItemCode(),
                        opening.getRewardItemType(),
                        opening.getRewardGoldAmount(),
                        opening.getOpenedAt()))
                .toList();
    }

    private Map<UUID, String> fetchDisplayNames(Set<UUID> userIds) {
        if (userIds.isEmpty()) {
            return Collections.emptyMap();
        }

        try {
            List<UserIdNamePair> pairs = userProfileClient.post()
                    .uri("/api/profiles/batch-display-names")
                    .bodyValue(userIds)
                    .retrieve()
                    .bodyToFlux(UserIdNamePair.class)
                    .collectList()
                    .block();

            if (pairs == null) {
                return Collections.emptyMap();
            }

            return pairs.stream()
                    .collect(Collectors.toMap(
                            UserIdNamePair::userId,
                            UserIdNamePair::displayName));

        } catch (Exception e) {
            System.err.println("Failed to fetch display names: " + e.getMessage());
            return Collections.emptyMap();
        }
    }

    private record AddDiscRequest(String itemCode, boolean equip) {
    }

    private record AddDiscWithResultResponse(Object profile, boolean alreadyOwned) {
    }

    private record DailyFreeAvailabilityResponse(boolean available) {
    }

    private record UserIdNamePair(UUID userId, String displayName) {
    }
}
