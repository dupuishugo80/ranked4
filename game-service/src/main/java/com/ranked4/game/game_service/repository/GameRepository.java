package com.ranked4.game.game_service.repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    @Query("SELECT g FROM Game g WHERE g.ranked = true AND g.status = :status AND g.finishedAt IS NOT NULL ORDER BY g.finishedAt DESC LIMIT 5")
    List<Game> findTop5RankedFinishedGames(@Param("status") GameStatus status);

    @Query("SELECT g FROM Game g WHERE g.status = :status AND g.turnStartTime IS NOT NULL AND g.turnStartTime <= :threshold")
    List<Game> findGamesWithExpiredTurns(@Param("status") GameStatus status, @Param("threshold") Instant threshold);

    @Query("SELECT g FROM Game g WHERE g.status = :status AND (g.playerOneId = :userId OR g.playerTwoId = :userId) AND g.finishedAt IS NOT NULL")
    Page<Game> findByUserIdAndStatus(
            @Param("userId") UUID userId,
            @Param("status") GameStatus status,
            Pageable pageable);

    @Query("SELECT g FROM Game g WHERE g.finishedAt IS NULL AND g.createdAt <= :threshold")
    List<Game> findBuggedGames(@Param("threshold") Instant threshold);
}
