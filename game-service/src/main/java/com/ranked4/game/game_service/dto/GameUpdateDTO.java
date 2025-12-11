package com.ranked4.game.game_service.dto;

import java.time.Duration;
import java.time.Instant;
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
        PlayerInfoDTO playerTwo,
        Integer aiDifficulty,
        Long turnTimeRemainingSeconds) {
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
                playerTwo,
                game.getAiDifficulty(),
                calculateTimeRemaining(game));
    }

    private static Long calculateTimeRemaining(Game game) {
        if (game.getTurnStartTime() == null || game.getStatus() != GameStatus.IN_PROGRESS) {
            return null;
        }

        long elapsed = Duration.between(game.getTurnStartTime(), Instant.now()).getSeconds();
        long remaining = 45 - elapsed;
        return Math.max(0, remaining);
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
                playerTwo,
                aiDifficulty,
                turnTimeRemainingSeconds);
    }
}