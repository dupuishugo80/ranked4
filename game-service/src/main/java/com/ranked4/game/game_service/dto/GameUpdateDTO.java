package com.ranked4.game.game_service.dto;

import java.util.UUID;

import com.ranked4.game.game_service.model.Disc;
import com.ranked4.game.game_service.model.Game;
import com.ranked4.game.game_service.model.GameStatus;

public class GameUpdateDTO {

    private UUID gameId;
    private String boardState;
    private GameStatus status;
    private Disc nextPlayer;
    private Disc winner;
    private UUID playerOneId;
    private UUID playerTwoId;
    private String error;
    private String origin;
    private PlayerInfoDTO playerOne;
    private PlayerInfoDTO playerTwo;

    public GameUpdateDTO(Game game, PlayerInfoDTO playerOne, PlayerInfoDTO playerTwo) {
        this.gameId = game.getGameId();
        this.boardState = game.getBoardState();
        this.status = game.getStatus();
        this.nextPlayer = game.getNextPlayer();
        this.winner = game.getWinner();
        this.playerOneId = game.getPlayerOneId();
        this.playerTwoId = game.getPlayerTwoId();
        this.error = null;
        this.origin = game.getOrigin();
        this.playerOne = playerOne;
        this.playerTwo = playerTwo;
    }
    
    public UUID getGameId() {
        return gameId;
    }

    public void setGameId(UUID gameId) {
        this.gameId = gameId;
    }

    public String getBoardState() {
        return boardState;
    }

    public void setBoardState(String boardState) {
        this.boardState = boardState;
    }

    public GameStatus getStatus() {
        return status;
    }

    public void setStatus(GameStatus status) {
        this.status = status;
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

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public PlayerInfoDTO getPlayerOne() {
        return playerOne;
    }

    public void setPlayerOne(PlayerInfoDTO playerOne) {
        this.playerOne = playerOne;
    }

    public PlayerInfoDTO getPlayerTwo() {
        return playerTwo;
    }

    public void setPlayerTwo(PlayerInfoDTO playerTwo) {
        this.playerTwo = playerTwo;
    }
}