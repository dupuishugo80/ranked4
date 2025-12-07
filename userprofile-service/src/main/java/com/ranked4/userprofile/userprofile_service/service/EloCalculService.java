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

    public EloCalculService(UserProfileRepository userProfileRepository) {
        this.userProfileRepository = userProfileRepository;
    }

    @KafkaListener(topics = "game.finished", groupId = "${spring.kafka.consumer.group-id}", containerFactory = "gameFinishedListenerContainerFactory")

    @Transactional
    public void handleGameFinished(GameFinishedEvent event) {
        boolean isRanked = event.isRanked() && (event.getOrigin() == null || "RANKED".equals(event.getOrigin()));

        if (!isRanked) {
            return;
        }

        log.info("GameFinishedEvent event received: {}", event);

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
                profile1.setDraws(profile1.getDraws() + 1);
                profile2.setDraws(profile2.getDraws() + 1);
                profile1.setGold(profile1.getGold() + goldRewardDraw);
                profile2.setGold(profile2.getGold() + goldRewardDraw);
                log.info("Result: Draw ({} game)", isRanked ? "Ranked" : "Unranked");

            } else if (event.getWinner().equals("PLAYER_ONE")) {
                scorePlayer1 = 1.0;
                scorePlayer2 = 0.0;
                profile1.setWins(profile1.getWins() + 1);
                profile2.setLosses(profile2.getLosses() + 1);
                profile1.setGold(profile1.getGold() + goldRewardWin);
                log.info("Result: Player 1 wins ({} game)", isRanked ? "Ranked" : "Unranked");

            } else {
                scorePlayer1 = 0.0;
                scorePlayer2 = 1.0;
                profile1.setLosses(profile1.getLosses() + 1);
                profile2.setWins(profile2.getWins() + 1);
                profile2.setGold(profile2.getGold() + goldRewardWin);
                log.info("Result: Player 2 wins ({} game)", isRanked ? "Ranked" : "Unranked");
            }

            if (isRanked) {
                calculateNewElo(profile1, profile2, scorePlayer1, scorePlayer2);
            }

            profile1.setGamesPlayed(profile1.getGamesPlayed() + 1);
            profile2.setGamesPlayed(profile2.getGamesPlayed() + 1);

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
}
