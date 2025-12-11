package com.ranked4.game.game_service.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ranked4.game.game_service.dto.GameHistoryDTO;
import com.ranked4.game.game_service.dto.PveGameResponse;
import com.ranked4.game.game_service.model.Game;
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

    @GetMapping("/history/user/{userId}")
    public ResponseEntity<Page<GameHistoryDTO>> getUserGameHistory(
        @PathVariable UUID userId,
        @PageableDefault(size = 10, sort = "finishedAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<GameHistoryDTO> history = gameService.getUserGameHistory(userId, pageable);
        return ResponseEntity.ok(history);
    }

    @PostMapping("/pve")
    public ResponseEntity<PveGameResponse> createPveGame(
        @RequestHeader("X-User-Id") UUID userId,
        @RequestParam(defaultValue = "2") int difficulty
    ) {
        if (difficulty < 1 || difficulty > 3) {
            return ResponseEntity.badRequest().build();
        }

        UUID gameId = UUID.randomUUID();
        Game game = gameService.createPveGame(gameId, userId, difficulty);

        return ResponseEntity.ok(new PveGameResponse(
            game.getGameId(),
            userId,
            difficulty
        ));
    }
}
