package com.ranked4.game.game_service.dto;

import java.util.UUID;

public class GifReactionMessage {
    
      private UUID gameId;
      private UUID playerId;
      private String gifCode;

      public UUID getGameId() {
          return gameId;
      }

      public void setGameId(UUID gameId) {
          this.gameId = gameId;
      }

      public UUID getPlayerId() {
          return playerId;
      }

      public void setPlayerId(UUID playerId) {
          this.playerId = playerId;
      }

      public String getGifCode() {
          return gifCode;
      }

      public void setGifCode(String gifCode) {
          this.gifCode = gifCode;
      }
  }