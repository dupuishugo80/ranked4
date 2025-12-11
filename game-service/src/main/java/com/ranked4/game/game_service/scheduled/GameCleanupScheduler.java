package com.ranked4.game.game_service.scheduled;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ranked4.game.game_service.model.Game;
import com.ranked4.game.game_service.repository.GameRepository;

@Component
public class GameCleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(GameCleanupScheduler.class);
    private final GameRepository gameRepository;

    public GameCleanupScheduler(GameRepository gameRepository) {
        this.gameRepository = gameRepository;
    }

    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void cleanupBuggedGames() {
        Instant threshold = Instant.now().minus(30, ChronoUnit.MINUTES);

        List<Game> buggedGames = gameRepository.findBuggedGames(threshold);

        if (!buggedGames.isEmpty()) {
            log.warn("Suppression de {} parties buguées (créées avant {} et jamais terminées)",
                    buggedGames.size(), threshold);
            gameRepository.deleteAll(buggedGames);
            log.info("Nettoyage terminé : {} parties supprimées", buggedGames.size());
        } else {
            log.debug("Aucune partie buguée à nettoyer");
        }
    }
}
