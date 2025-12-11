package com.ranked4.ai.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
public class Connect4AiService {
    private static final int ROWS = 6;
    private static final int COLS = 7;
    private static final int WIN_LENGTH = 4;
    private static final int MAX_SCORE = 1000000;
    private static final Random random = new Random();

    public int calculateBestMove(String grid, int difficulty, int aiPlayerId) {
        int opponentId = aiPlayerId == 1 ? 2 : 1;

        int maxDepth = switch (difficulty) {
            case 1 -> 4; // Easy
            case 2 -> 5; // Medium
            case 3 -> 6; // Hard
            case 4 -> 8; // Very Hard
            default -> 5;
        };

        if (difficulty == 1 && random.nextInt(100) < 80) {
            List<Integer> validMoves = getValidMoves(grid);
            return validMoves.get(random.nextInt(validMoves.size()));
        }

        List<Integer> validMoves = getValidMoves(grid);
        if (validMoves.isEmpty()) {
            throw new IllegalStateException("No valid moves available");
        }

        int bestMove = validMoves.get(0);
        int bestScore = Integer.MIN_VALUE;

        for (int col : validMoves) {
            String newGrid = makeMove(grid, col, aiPlayerId);
            int score = minimax(newGrid, maxDepth - 1, false, aiPlayerId, opponentId, Integer.MIN_VALUE,
                    Integer.MAX_VALUE);

            if (score > bestScore) {
                bestScore = score;
                bestMove = col;
            }
        }

        return bestMove;
    }

    private int minimax(String grid, int depth, boolean isMaximizing, int aiPlayerId, int opponentId, int alpha,
            int beta) {
        Integer winner = checkWinner(grid);
        if (winner != null) {
            if (winner == aiPlayerId)
                return MAX_SCORE - depth;
            if (winner == opponentId)
                return -MAX_SCORE + depth;
            return 0;
        }

        List<Integer> validMoves = getValidMoves(grid);
        if (depth == 0 || validMoves.isEmpty()) {
            return evaluatePosition(grid, aiPlayerId, opponentId);
        }

        if (isMaximizing) {
            int maxScore = Integer.MIN_VALUE;
            for (int col : validMoves) {
                String newGrid = makeMove(grid, col, aiPlayerId);
                int score = minimax(newGrid, depth - 1, false, aiPlayerId, opponentId, alpha, beta);
                maxScore = Math.max(maxScore, score);
                alpha = Math.max(alpha, score);
                if (beta <= alpha)
                    break;
            }
            return maxScore;
        } else {
            int minScore = Integer.MAX_VALUE;
            for (int col : validMoves) {
                String newGrid = makeMove(grid, col, opponentId);
                int score = minimax(newGrid, depth - 1, true, aiPlayerId, opponentId, alpha, beta);
                minScore = Math.min(minScore, score);
                beta = Math.min(beta, score);
                if (beta <= alpha)
                    break;
            }
            return minScore;
        }
    }

    private int evaluatePosition(String grid, int aiPlayerId, int opponentId) {
        int score = 0;

        score += evaluateCenter(grid, aiPlayerId, opponentId);

        score += evaluateWindows(grid, aiPlayerId, opponentId);

        return score;
    }

    private int evaluateCenter(String grid, int aiPlayerId, int opponentId) {
        int centerCol = COLS / 2;
        int centerCount = 0;

        for (int row = 0; row < ROWS; row++) {
            int index = row * COLS + centerCol;
            char cell = grid.charAt(index);
            if (cell == Character.forDigit(aiPlayerId, 10)) {
                centerCount += 3;
            } else if (cell == Character.forDigit(opponentId, 10)) {
                centerCount -= 3;
            }
        }

        return centerCount;
    }

    private int evaluateWindows(String grid, int aiPlayerId, int opponentId) {
        int score = 0;

        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col <= COLS - WIN_LENGTH; col++) {
                List<Character> window = new ArrayList<>();
                for (int i = 0; i < WIN_LENGTH; i++) {
                    window.add(grid.charAt(row * COLS + col + i));
                }
                score += evaluateWindow(window, aiPlayerId, opponentId);
            }
        }

        for (int col = 0; col < COLS; col++) {
            for (int row = 0; row <= ROWS - WIN_LENGTH; row++) {
                List<Character> window = new ArrayList<>();
                for (int i = 0; i < WIN_LENGTH; i++) {
                    window.add(grid.charAt((row + i) * COLS + col));
                }
                score += evaluateWindow(window, aiPlayerId, opponentId);
            }
        }

        for (int row = 0; row <= ROWS - WIN_LENGTH; row++) {
            for (int col = 0; col <= COLS - WIN_LENGTH; col++) {
                List<Character> window = new ArrayList<>();
                for (int i = 0; i < WIN_LENGTH; i++) {
                    window.add(grid.charAt((row + i) * COLS + col + i));
                }
                score += evaluateWindow(window, aiPlayerId, opponentId);
            }
        }

        for (int row = WIN_LENGTH - 1; row < ROWS; row++) {
            for (int col = 0; col <= COLS - WIN_LENGTH; col++) {
                List<Character> window = new ArrayList<>();
                for (int i = 0; i < WIN_LENGTH; i++) {
                    window.add(grid.charAt((row - i) * COLS + col + i));
                }
                score += evaluateWindow(window, aiPlayerId, opponentId);
            }
        }

        return score;
    }

    private int evaluateWindow(List<Character> window, int aiPlayerId, int opponentId) {
        char aiChar = Character.forDigit(aiPlayerId, 10);
        char oppChar = Character.forDigit(opponentId, 10);

        long aiCount = window.stream().filter(c -> c == aiChar).count();
        long oppCount = window.stream().filter(c -> c == oppChar).count();
        long emptyCount = window.stream().filter(c -> c == '0').count();

        if (aiCount == 4)
            return 100;
        if (oppCount == 4)
            return -100;

        if (aiCount == 3 && emptyCount == 1)
            return 5;
        if (aiCount == 2 && emptyCount == 2)
            return 2;

        if (oppCount == 3 && emptyCount == 1)
            return -50;
        if (oppCount == 2 && emptyCount == 2)
            return -2;

        return 0;
    }

    private Integer checkWinner(String grid) {
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col <= COLS - WIN_LENGTH; col++) {
                char first = grid.charAt(row * COLS + col);
                if (first != '0') {
                    boolean win = true;
                    for (int i = 1; i < WIN_LENGTH; i++) {
                        if (grid.charAt(row * COLS + col + i) != first) {
                            win = false;
                            break;
                        }
                    }
                    if (win)
                        return Character.getNumericValue(first);
                }
            }
        }

        for (int col = 0; col < COLS; col++) {
            for (int row = 0; row <= ROWS - WIN_LENGTH; row++) {
                char first = grid.charAt(row * COLS + col);
                if (first != '0') {
                    boolean win = true;
                    for (int i = 1; i < WIN_LENGTH; i++) {
                        if (grid.charAt((row + i) * COLS + col) != first) {
                            win = false;
                            break;
                        }
                    }
                    if (win)
                        return Character.getNumericValue(first);
                }
            }
        }

        for (int row = 0; row <= ROWS - WIN_LENGTH; row++) {
            for (int col = 0; col <= COLS - WIN_LENGTH; col++) {
                char first = grid.charAt(row * COLS + col);
                if (first != '0') {
                    boolean win = true;
                    for (int i = 1; i < WIN_LENGTH; i++) {
                        if (grid.charAt((row + i) * COLS + col + i) != first) {
                            win = false;
                            break;
                        }
                    }
                    if (win)
                        return Character.getNumericValue(first);
                }
            }
        }

        for (int row = WIN_LENGTH - 1; row < ROWS; row++) {
            for (int col = 0; col <= COLS - WIN_LENGTH; col++) {
                char first = grid.charAt(row * COLS + col);
                if (first != '0') {
                    boolean win = true;
                    for (int i = 1; i < WIN_LENGTH; i++) {
                        if (grid.charAt((row - i) * COLS + col + i) != first) {
                            win = false;
                            break;
                        }
                    }
                    if (win)
                        return Character.getNumericValue(first);
                }
            }
        }

        if (!grid.contains("0"))
            return 0;

        return null;
    }

    private List<Integer> getValidMoves(String grid) {
        List<Integer> validMoves = new ArrayList<>();
        for (int col = 0; col < COLS; col++) {
            if (grid.charAt(col) == '0') {
                validMoves.add(col);
            }
        }
        return validMoves;
    }

    private String makeMove(String grid, int col, int playerId) {
        char[] gridArray = grid.toCharArray();

        for (int row = ROWS - 1; row >= 0; row--) {
            int index = row * COLS + col;
            if (gridArray[index] == '0') {
                gridArray[index] = Character.forDigit(playerId, 10);
                break;
            }
        }

        return new String(gridArray);
    }
}
