package com.ranked4.game.game_service.dto;

import java.util.UUID;

import com.ranked4.game.game_service.model.Disc;
import com.ranked4.game.game_service.model.GameType;

public class GameFinishedEvent {

    private UUID gameId;
    private UUID playerOneId;
    private UUID playerTwoId;
    private String winner;
    private boolean ranked = true;
    private String origin = "RANKED";
    private String gameType = "PVP_RANKED";
    private Integer aiDifficulty;

    public GameFinishedEvent() {
    }

    public GameFinishedEvent(UUID gameId, UUID playerOneId, UUID playerTwoId, Disc winnerDisc) {
        this(gameId, playerOneId, playerTwoId, winnerDisc, true, "RANKED", GameType.PVP_RANKED);
    }

    public GameFinishedEvent(UUID gameId, UUID playerOneId, UUID playerTwoId, Disc winnerDisc, boolean ranked, String origin) {
        this(gameId, playerOneId, playerTwoId, winnerDisc, ranked, origin, GameType.PVP_RANKED);
    }

    public GameFinishedEvent(UUID gameId, UUID playerOneId, UUID playerTwoId, Disc winnerDisc, boolean ranked, String origin, GameType gameType) {
        this.gameId = gameId;
        this.playerOneId = playerOneId;
        this.playerTwoId = playerTwoId;
        this.ranked = ranked;
        this.origin = origin;
        this.gameType = gameType != null ? gameType.name() : "PVP_RANKED";

        if (winnerDisc == null) {
            this.winner = null;
        } else {
            this.winner = winnerDisc.name();
        }
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

    public String getWinner() {
        return winner;
    }

    public void setWinner(String winner) {
        this.winner = winner;
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

    public String getGameType() {
        return gameType;
    }

    public void setGameType(String gameType) {
        this.gameType = gameType;
    }

    public Integer getAiDifficulty() {
        return aiDifficulty;
    }

    public void setAiDifficulty(Integer aiDifficulty) {
        this.aiDifficulty = aiDifficulty;
    }
}
