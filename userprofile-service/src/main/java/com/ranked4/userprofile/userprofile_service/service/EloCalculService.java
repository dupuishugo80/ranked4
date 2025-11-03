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

    public EloCalculService(UserProfileRepository userProfileRepository) {
        this.userProfileRepository = userProfileRepository;
    }

    @KafkaListener(
        topics = "game.finished",
        groupId = "${spring.kafka.consumer.group-id}",
        containerFactory = "gameFinishedListenerContainerFactory"
    )

    @Transactional
    public void handleGameFinished(GameFinishedEvent event) {
        log.info("GameFinishedEvent event received: {}", event);

        try {
            UserProfile profile1 = userProfileRepository.findByUserId(event.getPlayerOneId())
                .orElseThrow(() -> new IllegalStateException("Profile not found for player 1: " + event.getPlayerOneId()));

            UserProfile profile2 = userProfileRepository.findByUserId(event.getPlayerTwoId())
                .orElseThrow(() -> new IllegalStateException("Profile not found for player 2: " + event.getPlayerTwoId()));

            double scorePlayer1;
            double scorePlayer2;

            if (event.getWinner() == null) {
                scorePlayer1 = 0.5;
                scorePlayer2 = 0.5;
                profile1.setDraws(profile1.getDraws() + 1);
                profile2.setDraws(profile2.getDraws() + 1);
                log.info("Result: Draw");

            } else if (event.getWinner().equals("PLAYER_ONE")) {
                scorePlayer1 = 1.0;
                scorePlayer2 = 0.0;
                profile1.setWins(profile1.getWins() + 1);
                profile2.setLosses(profile2.getLosses() + 1);
                log.info("Result: Player 1 wins");

            } else {
                scorePlayer1 = 0.0;
                scorePlayer2 = 1.0;
                profile1.setLosses(profile1.getLosses() + 1);
                profile2.setWins(profile2.getWins() + 1);
                log.info("Result: Player 2 wins");
            }
            
            calculateNewElo(profile1, profile2, scorePlayer1, scorePlayer2);

            profile1.setGamesPlayed(profile1.getGamesPlayed() + 1);
            profile2.setGamesPlayed(profile2.getGamesPlayed() + 1);

            userProfileRepository.saveAll(List.of(profile1, profile2));

            log.info("ELO updated. Player 1: {} ({} ELO), Player 2: {} ({} ELO)",

            profile1.getDisplayName(), profile1.getElo(),
            profile2.getDisplayName(), profile2.getElo());

        } catch (Exception e) {
            log.error("Failed to calculate ELO for gameId {}: {}", event.getGameId(), e.getMessage());
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
