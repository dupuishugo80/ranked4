package com.ranked4.game.game_service.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ranked4.game.game_service.dto.GameHistoryDTO;
import com.ranked4.game.game_service.service.GameService;

@RestController
@RequestMapping("/api/game")
public class GameController {
    private final GameService gameService;

    public GameController(GameService gameService) {
        this.gameService = gameService;
    }

    @GetMapping("/history")
    public ResponseEntity<List<GameHistoryDTO>> getGameHistory() {
        List<GameHistoryDTO> history = gameService.getGameHistory();
        return ResponseEntity.ok(history);
    }
}
