package com.ranked4.ai.controller;

import com.ranked4.ai.dto.AiMoveRequest;
import com.ranked4.ai.dto.AiMoveResponse;
import com.ranked4.ai.service.Connect4AiService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final Connect4AiService aiService;

    public AiController(Connect4AiService aiService) {
        this.aiService = aiService;
    }

    @PostMapping("/next-move")
    public ResponseEntity<AiMoveResponse> getNextMove(@Valid @RequestBody AiMoveRequest request) {
        int bestMove = aiService.calculateBestMove(
            request.grid(),
            request.difficulty(),
            request.aiPlayerId()
        );

        return ResponseEntity.ok(new AiMoveResponse(bestMove));
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("AI Service is running");
    }
}
