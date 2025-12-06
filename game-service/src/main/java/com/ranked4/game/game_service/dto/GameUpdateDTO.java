package com.ranked4.game.game_service.dto;

import java.util.UUID;

import com.ranked4.game.game_service.model.Disc;
import com.ranked4.game.game_service.model.Game;
import com.ranked4.game.game_service.model.GameStatus;

public record GameUpdateDTO(
    UUID gameId,
    String boardState,
    GameStatus status,
    Disc nextPlayer,
    Disc winner,
    UUID playerOneId,
    UUID playerTwoId,
    String error,
    String origin,
    PlayerInfoDTO playerOne,
    PlayerInfoDTO playerTwo
) {
    public GameUpdateDTO(Game game, PlayerInfoDTO playerOne, PlayerInfoDTO playerTwo) {
        this(
            game.getGameId(),
            game.getBoardState(),
            game.getStatus(),
            game.getNextPlayer(),
            game.getWinner(),
            game.getPlayerOneId(),
            game.getPlayerTwoId(),
            null,
            game.getOrigin(),
            playerOne,
            playerTwo
        );
    }

    public GameUpdateDTO withError(String error) {
        return new GameUpdateDTO(
            gameId,
            boardState,
            status,
            nextPlayer,
            winner,
            playerOneId,
            playerTwoId,
            error,
            origin,
            playerOne,
            playerTwo
        );
    }
}