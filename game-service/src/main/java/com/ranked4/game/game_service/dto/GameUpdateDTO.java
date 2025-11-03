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

    public GameUpdateDTO() {
    }

    public static GameUpdateDTO fromEntity(Game game) {
        GameUpdateDTO dto = new GameUpdateDTO();
        dto.setGameId(game.getGameId());
        dto.setBoardState(game.getBoardState());
        dto.setStatus(game.getStatus());
        dto.setNextPlayer(game.getNextPlayer());
        dto.setWinner(game.getWinner());
        dto.setPlayerOneId(game.getPlayerOneId());
        dto.setPlayerTwoId(game.getPlayerTwoId());
        return dto;
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
}