package com.ranked4.game.game_service.model;

public enum GameType {
    PVP_RANKED,   // Ranked matchmaking (affects ELO)
    PVP_PRIVATE,  // Private lobby (no ELO)
    PVE           // Player vs AI (no ELO, reduced gold)
}
