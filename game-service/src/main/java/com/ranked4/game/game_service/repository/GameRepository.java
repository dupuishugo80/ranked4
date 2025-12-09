package com.ranked4.game.game_service.repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ranked4.game.game_service.model.Game;
import com.ranked4.game.game_service.model.GameStatus;

@Repository
public interface GameRepository extends JpaRepository<Game, UUID> {
    List<Game> findByStatusAndPlayerOneIdOrPlayerTwoId(GameStatus status, UUID playerOneId, UUID playerTwoId);
    List<Game> findByStatus(GameStatus status);
    List<Game> findTop5ByRankedTrueAndStatusOrderByFinishedAtDesc(GameStatus status);

    @Query("SELECT g FROM Game g WHERE g.status = :status AND g.turnStartTime IS NOT NULL AND g.turnStartTime <= :threshold")
    List<Game> findGamesWithExpiredTurns(@Param("status") GameStatus status, @Param("threshold") Instant threshold);
}
