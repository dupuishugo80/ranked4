package com.ranked4.game.game_service.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ranked4.game.game_service.model.Game;
import com.ranked4.game.game_service.service.GameService;

@RestController
@RequestMapping("/api/game")
public class GameTestController {

    private final GameService gameService;

    public GameTestController(GameService gameService) {
        this.gameService = gameService;
    }

    @PostMapping("/create")
    public ResponseEntity<Game> createTestGame() {
        UUID playerOne = UUID.randomUUID();
        UUID playerTwo = UUID.randomUUID();
        
        Game newGame = gameService.createGame(playerOne, playerTwo);
        
        return ResponseEntity.ok(newGame);
    }
}