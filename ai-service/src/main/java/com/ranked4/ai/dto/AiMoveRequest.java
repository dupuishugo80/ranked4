package com.ranked4.ai.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record AiMoveRequest(
        @NotBlank(message = "Grid is required") @Pattern(regexp = "^[012]{42}$", message = "Grid must be 42 characters of 0, 1, or 2") String grid,

        @Min(value = 1, message = "Difficulty must be between 1 and 4") @Max(value = 4, message = "Difficulty must be between 1 and 4") int difficulty,

        @Min(value = 1, message = "AI player ID must be 1 or 2") @Max(value = 2, message = "AI player ID must be 1 or 2") int aiPlayerId) {
}