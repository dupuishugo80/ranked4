package com.ranked4.game.game_service.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ranked4.game.game_service.model.Game;
import com.ranked4.game.game_service.model.GameStatus;

@Repository
public interface GameRepository extends JpaRepository<Game, UUID> {
    List<Game> findByStatusAndPlayerOneIdOrPlayerTwoId(GameStatus status, UUID playerOneId, UUID playerTwoId);
    List<Game> findByStatus(GameStatus status);
    List<Game> findTop5ByRankedTrueAndStatusOrderByFinishedAtDesc(GameStatus status);
}
