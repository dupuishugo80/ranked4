package com.ranked4.game.game_service.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ranked4.game.game_service.model.Move;

@Repository
public interface MoveRepository extends JpaRepository<Move, Long> {
    List<Move> findByGameGameIdOrderByMoveNumberAsc(UUID gameId);
}