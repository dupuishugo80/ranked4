package com.ranked4.game.game_service.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "game_moves", indexes = {
    @Index(name = "idx_move_gameid", columnList = "game_id")
})
public class Move {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Disc player;

    @Column(nullable = false)
    private int moveColumn;

    @Column(nullable = false)
    private int moveNumber;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public Move() {
    }

    public Move(Game game, Disc player, int moveColumn, int moveNumber) {
        this.game = game;
        this.player = player;
        this.moveColumn = moveColumn;
        this.moveNumber = moveNumber;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Game getGame() {
        return game;
    }

    public void setGame(Game game) {
        this.game = game;
    }

    public Disc getPlayer() {
        return player;
    }

    public void setPlayer(Disc player) {
        this.player = player;
    }

    public int getMoveColumn() {
        return moveColumn;
    }

    public void setMoveColumn(int moveColumn) {
        this.moveColumn = moveColumn;
    }

    public int getMoveNumber() {
        return moveNumber;
    }

    public void setMoveNumber(int moveNumber) {
        this.moveNumber = moveNumber;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}