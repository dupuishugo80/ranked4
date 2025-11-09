package com.ranked4.game.game_service.model;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

@Entity
@Table(name = "games")
public class Game {

    @Id
    private UUID gameId;

    @Column(nullable = false)
    private UUID playerOneId;

    @Column(nullable = false)
    private UUID playerTwoId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GameStatus status = GameStatus.IN_PROGRESS;

    @Column(nullable = false, length = 42)
    private String boardState;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Disc nextPlayer = Disc.PLAYER_ONE;

    @Enumerated(EnumType.STRING)
    private Disc winner;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    private Instant finishedAt;

    @Column(nullable = false)
    private boolean ranked = true;

    @Column(nullable = false, length = 32)
    private String origin = "RANKED";

    @Transient
    private GameBoard gameLogic = new GameBoard();

    @PostLoad
    private void initializeLogicAfterLoad() {
        if (this.gameLogic == null) {
            this.gameLogic = new GameBoard();
        }
        this.gameLogic.deserializeGrid(this.boardState, this.nextPlayer);
    }

    public void startGame(UUID gameId, UUID playerOneId, UUID playerTwoId) {
        this.gameId = gameId;
        this.playerOneId = playerOneId;
        this.playerTwoId = playerTwoId;
        this.status = GameStatus.IN_PROGRESS;
        this.nextPlayer = Disc.PLAYER_ONE;
        this.gameLogic = new GameBoard();
        this.boardState = this.gameLogic.serializeGrid();
    }

    public boolean applyMove(int column, Disc playerDisc) {
        if (this.status != GameStatus.IN_PROGRESS) {
            throw new IllegalStateException("Game is already finished.");
        }

        if (playerDisc != this.nextPlayer) {
            throw new IllegalStateException("It's not " + playerDisc + "'s turn.");
        }

        boolean success = this.gameLogic.applyMove(column);

        if (success) {
            this.boardState = this.gameLogic.serializeGrid();
            this.nextPlayer = this.gameLogic.getNextPlayer();
            this.status = this.gameLogic.getStatus();
            this.winner = this.gameLogic.getWinner();

            if (this.status == GameStatus.FINISHED) {
                this.finishedAt = Instant.now();
            }
        }
        return success;
    }

    public void forfeit(Disc forfeitingPlayer) {
        if (this.status != GameStatus.IN_PROGRESS) {
            return;
        }

        this.status = GameStatus.FINISHED;
        this.finishedAt = Instant.now();
        
        this.winner = (forfeitingPlayer == Disc.PLAYER_ONE) ? Disc.PLAYER_TWO : Disc.PLAYER_ONE;
        
        this.gameLogic.setStatus(GameStatus.FINISHED);
        this.gameLogic.setWinner(this.winner);
    }

    public UUID getGameId() {
        return gameId;
    }

    public void setGameId(UUID gameId) {
        this.gameId = gameId;
    }

    public UUID getPlayerOneId() {
        return playerOneId;
    }

    public void setPlayerOneId(UUID playerOneId) {
        this.playerOneId = playerOneId;
    }

    public UUID getPlayerTwoId() {
        return playerTwoId;
    }

    public void setPlayerTwoId(UUID playerTwoId) {
        this.playerTwoId = playerTwoId;
    }

    public GameStatus getStatus() {
        return status;
    }

    public void setStatus(GameStatus status) {
        this.status = status;
    }

    public String getBoardState() {
        return boardState;
    }

    public void setBoardState(String boardState) {
        this.boardState = boardState;
    }

    public Disc getNextPlayer() {
        return nextPlayer;
    }

    public void setNextPlayer(Disc nextPlayer) {
        this.nextPlayer = nextPlayer;
    }

    public Disc getWinner() {
        return winner;
    }

    public void setWinner(Disc winner) {
        this.winner = winner;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(Instant finishedAt) {
        this.finishedAt = finishedAt;
    }

    public boolean isRanked() {
        return ranked;
    }

    public void setRanked(boolean ranked) {
        this.ranked = ranked;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public GameBoard getGameLogic() {
        return gameLogic;
    }

    public void setGameLogic(GameBoard gameLogic) {
        this.gameLogic = gameLogic;
    }
}