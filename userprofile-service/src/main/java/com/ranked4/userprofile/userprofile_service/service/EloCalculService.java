package com.ranked4.userprofile.userprofile_service.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ranked4.userprofile.userprofile_service.dto.GameFinishedEvent;
import com.ranked4.userprofile.userprofile_service.model.UserProfile;
import com.ranked4.userprofile.userprofile_service.repository.UserProfileRepository;

@Service
public class EloCalculService {

    private static final Logger log = LoggerFactory.getLogger(EloCalculService.class);
    private final UserProfileRepository userProfileRepository;

    private static final int K_FACTOR = 30;

    private static final int GOLD_ON_WIN_RANKED = 100;
    private static final int GOLD_ON_DRAW_RANKED = 50;

    private static final int GOLD_ON_WIN_UNRANKED = 50;
    private static final int GOLD_ON_DRAW_UNRANKED = 25;

    private static final int GOLD_ON_WIN_PVE_EASY = 50;
    private static final int GOLD_ON_DRAW_PVE_EASY = 25;
    private static final int GOLD_ON_WIN_PVE_MEDIUM = 100;
    private static final int GOLD_ON_DRAW_PVE_MEDIUM = 50;
    private static final int GOLD_ON_WIN_PVE_HARD = 200;
    private static final int GOLD_ON_DRAW_PVE_HARD = 100;

    private static final java.util.UUID AI_PLAYER_UUID = java.util.UUID
            .fromString("00000000-0000-0000-0000-000000000001");

    public EloCalculService(UserProfileRepository userProfileRepository) {
        this.userProfileRepository = userProfileRepository;
    }

    @KafkaListener(topics = "game.finished", groupId = "${spring.kafka.consumer.group-id}", containerFactory = "gameFinishedListenerContainerFactory")

    @Transactional
    public void handleGameFinished(GameFinishedEvent event) {
        log.info("GameFinishedEvent received: {}", event);

        if ("PVE".equals(event.getGameType())) {
            handlePveGame(event);
            return;
        }

        boolean isRanked = event.isRanked() && (event.getOrigin() == null || "RANKED".equals(event.getOrigin()));

        log.info("Processing {} game: {}", isRanked ? "ranked" : "unranked", event);

        try {
            UserProfile profile1 = userProfileRepository.findByUserId(event.getPlayerOneId())
                    .orElseThrow(() -> new IllegalStateException(
                            "Profile not found for player 1: " + event.getPlayerOneId()));

            UserProfile profile2 = userProfileRepository.findByUserId(event.getPlayerTwoId())
                    .orElseThrow(() -> new IllegalStateException(
                            "Profile not found for player 2: " + event.getPlayerTwoId()));

            double scorePlayer1;
            double scorePlayer2;

            int goldRewardWin = isRanked ? GOLD_ON_WIN_RANKED : GOLD_ON_WIN_UNRANKED;
            int goldRewardDraw = isRanked ? GOLD_ON_DRAW_RANKED : GOLD_ON_DRAW_UNRANKED;

            if (event.getWinner() == null) {
                scorePlayer1 = 0.5;
                scorePlayer2 = 0.5;
                if (isRanked) {
                    profile1.setDraws(profile1.getDraws() + 1);
                    profile2.setDraws(profile2.getDraws() + 1);
                }
                profile1.setGold(profile1.getGold() + goldRewardDraw);
                profile2.setGold(profile2.getGold() + goldRewardDraw);
                log.info("Result: Draw ({} game) - stats {}",
                        isRanked ? "Ranked" : "Unranked",
                        isRanked ? "updated" : "not affected");

            } else if (event.getWinner().equals("PLAYER_ONE")) {
                scorePlayer1 = 1.0;
                scorePlayer2 = 0.0;
                if (isRanked) {
                    profile1.setWins(profile1.getWins() + 1);
                    profile2.setLosses(profile2.getLosses() + 1);
                }
                profile1.setGold(profile1.getGold() + goldRewardWin);
                log.info("Result: Player 1 wins ({} game) - stats {}",
                        isRanked ? "Ranked" : "Unranked",
                        isRanked ? "updated" : "not affected");

            } else {
                scorePlayer1 = 0.0;
                scorePlayer2 = 1.0;
                if (isRanked) {
                    profile1.setLosses(profile1.getLosses() + 1);
                    profile2.setWins(profile2.getWins() + 1);
                }
                profile2.setGold(profile2.getGold() + goldRewardWin);
                log.info("Result: Player 2 wins ({} game) - stats {}",
                        isRanked ? "Ranked" : "Unranked",
                        isRanked ? "updated" : "not affected");
            }

            if (isRanked) {
                calculateNewElo(profile1, profile2, scorePlayer1, scorePlayer2);
                profile1.setGamesPlayed(profile1.getGamesPlayed() + 1);
                profile2.setGamesPlayed(profile2.getGamesPlayed() + 1);
            }

            userProfileRepository.saveAll(List.of(profile1, profile2));

            if (isRanked) {
                log.info(
                        "ELO and Gold updated (Ranked). Player 1: {} ({} ELO, {} Gold), Player 2: {} ({} ELO, {} Gold)",
                        profile1.getDisplayName(), profile1.getElo(), profile1.getGold(),
                        profile2.getDisplayName(), profile2.getElo(), profile2.getGold());
            } else {
                log.info("Gold updated (Unranked). Player 1: {} ({} Gold), Player 2: {} ({} Gold)",
                        profile1.getDisplayName(), profile1.getGold(),
                        profile2.getDisplayName(), profile2.getGold());
            }

        } catch (Exception e) {
            log.error("Failed to process game result for gameId {}: {}", event.getGameId(), e.getMessage());
        }
    }

    private void calculateNewElo(UserProfile playerA, UserProfile playerB, double scoreA, double scoreB) {
        int eloA = playerA.getElo();
        int eloB = playerB.getElo();

        double expectedA = 1.0 / (1.0 + Math.pow(10.0, (double) (eloB - eloA) / 400.0));
        double expectedB = 1.0 / (1.0 + Math.pow(10.0, (double) (eloA - eloB) / 400.0));

        int newEloA = (int) Math.round(eloA + K_FACTOR * (scoreA - expectedA));
        int newEloB = (int) Math.round(eloB + K_FACTOR * (scoreB - expectedB));

        log.debug("Calcul ELO J1: {} + {} * ({} - {}) = {}", eloA, K_FACTOR, scoreA, expectedA, newEloA);
        log.debug("Calcul ELO J2: {} + {} * ({} - {}) = {}", eloB, K_FACTOR, scoreB, expectedB, newEloB);

        playerA.setElo(newEloA);
        playerB.setElo(newEloB);
    }

    private int[] getPveGoldRewards(Integer difficulty) {
        if (difficulty == null) {
            difficulty = 2;
        }

        return switch (difficulty) {
            case 1 -> new int[] { GOLD_ON_WIN_PVE_EASY, GOLD_ON_DRAW_PVE_EASY }; // Easy: 50/25
            case 2 -> new int[] { GOLD_ON_WIN_PVE_MEDIUM, GOLD_ON_DRAW_PVE_MEDIUM }; // Medium: 100/50
            case 3 -> new int[] { GOLD_ON_WIN_PVE_HARD, GOLD_ON_DRAW_PVE_HARD }; // Hard: 200/100
            default -> new int[] { GOLD_ON_WIN_PVE_MEDIUM, GOLD_ON_DRAW_PVE_MEDIUM }; // Default to medium
        };
    }

    private void handlePveGame(GameFinishedEvent event) {
        log.info("Processing PVE game: {} (difficulty: {})", event.getGameId(), event.getAiDifficulty());

        java.util.UUID humanPlayerId;
        boolean humanIsPlayerOne;

        if (AI_PLAYER_UUID.equals(event.getPlayerOneId())) {
            humanPlayerId = event.getPlayerTwoId();
            humanIsPlayerOne = false;
        } else {
            humanPlayerId = event.getPlayerOneId();
            humanIsPlayerOne = true;
        }

        int[] goldRewards = getPveGoldRewards(event.getAiDifficulty());
        int goldOnWin = goldRewards[0];
        int goldOnDraw = goldRewards[1];

        try {
            UserProfile humanProfile = userProfileRepository.findByUserId(humanPlayerId)
                    .orElseThrow(() -> new IllegalStateException(
                            "Profile not found for player: " + humanPlayerId));

            if (event.getWinner() == null) {
                humanProfile.setGold(humanProfile.getGold() + goldOnDraw);
                log.info("PVE Draw (difficulty {}): {} earned {} gold (stats not affected)",
                        event.getAiDifficulty(), humanProfile.getDisplayName(), goldOnDraw);

            } else if ((humanIsPlayerOne && "PLAYER_ONE".equals(event.getWinner())) ||
                    (!humanIsPlayerOne && "PLAYER_TWO".equals(event.getWinner()))) {
                humanProfile.setGold(humanProfile.getGold() + goldOnWin);
                log.info("PVE Win (difficulty {}): {} earned {} gold (stats not affected)",
                        event.getAiDifficulty(), humanProfile.getDisplayName(), goldOnWin);

            } else {
                log.info("PVE Loss (difficulty {}): {} earned no gold (stats not affected)",
                        event.getAiDifficulty(), humanProfile.getDisplayName());
            }

            userProfileRepository.save(humanProfile);

            log.info("PVE game processed. Player: {} (ELO unchanged: {}, Gold: {})",
                    humanProfile.getDisplayName(), humanProfile.getElo(), humanProfile.getGold());

        } catch (Exception e) {
            log.error("Failed to process PVE game {}: {}", event.getGameId(), e.getMessage());
        }
    }
}
