package com.ranked4.game.game_service.model;

public class GameBoard {

    public static final int ROWS = 6;
    public static final int COLS = 7;
    private static final int WIN_STREAK = 4;

    private Disc[][] grid;
    private GameStatus status;
    private Disc nextPlayer;
    private Disc winner;

    public GameBoard() {
        this.grid = new Disc[ROWS][COLS];
        reset();
    }

    public void reset() {
        this.status = GameStatus.IN_PROGRESS;
        this.nextPlayer = Disc.PLAYER_ONE;
        this.winner = null;
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                this.grid[r][c] = Disc.EMPTY;
            }
        }
    }

    public boolean applyMove(int col) {
        if (col < 0 || col >= COLS || status != GameStatus.IN_PROGRESS) {
            return false;
        }

        for (int r = ROWS - 1; r >= 0; r--) {
            if (this.grid[r][col] == Disc.EMPTY) {
                Disc currentPlayer = this.nextPlayer;
                this.grid[r][col] = currentPlayer;

                if (checkWin(r, col, currentPlayer)) {
                    this.status = GameStatus.FINISHED;
                    this.winner = currentPlayer;
                }

                else if (isBoardFull()) {
                    this.status = GameStatus.FINISHED;
                    this.winner = null;
                }

                else {
                    switchPlayer();
                }

                return true;
            }
        }

        return false;
    }

    private void switchPlayer() {
        this.nextPlayer = (this.nextPlayer == Disc.PLAYER_ONE) ? Disc.PLAYER_TWO : Disc.PLAYER_ONE;
    }

    private boolean isBoardFull() {
        for (int c = 0; c < COLS; c++) {
            if (this.grid[0][c] == Disc.EMPTY) {
                return false;
            }
        }
        return true;
    }

    private boolean checkWin(int r, int c, Disc player) {
        int count = 0;
        for (int col = 0; col < COLS; col++) {
            count = (this.grid[r][col] == player) ? count + 1 : 0;
            if (count >= WIN_STREAK) return true;
        }

        count = 0;
        for (int row = 0; row < ROWS; row++) {
            count = (this.grid[row][c] == player) ? count + 1 : 0;
            if (count >= WIN_STREAK) return true;
        }

        count = 0;
        for (int i = -WIN_STREAK + 1; i < WIN_STREAK; i++) {
            int row = r + i;
            int col = c + i;
            if (row >= 0 && row < ROWS && col >= 0 && col < COLS) {
                count = (this.grid[row][col] == player) ? count + 1 : 0;
                if (count >= WIN_STREAK) return true;
            }
        }

        count = 0;
        for (int i = -WIN_STREAK + 1; i < WIN_STREAK; i++) {
            int row = r - i;
            int col = c + i;
            if (row >= 0 && row < ROWS && col >= 0 && col < COLS) {
                count = (this.grid[row][col] == player) ? count + 1 : 0;
                if (count >= WIN_STREAK) return true;
            }
        }

        return false;
    }

    public String serializeGrid() {
        StringBuilder sb = new StringBuilder(ROWS * COLS);
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                sb.append(this.grid[r][c].getValue());
            }
        }
        return sb.toString();
    }

    public void deserializeGrid(String state, Disc nextPlayer) {
        if (state == null || state.length() != ROWS * COLS) {
            reset();
            return;
        }

        int i = 0;
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                this.grid[r][c] = Disc.fromInt(Character.getNumericValue(state.charAt(i)));
                i++;
            }
        }
        this.nextPlayer = nextPlayer;
        this.status = GameStatus.IN_PROGRESS;
    }

    public Disc[][] getGrid() { return grid; }
    public GameStatus getStatus() { return status; }
    public void setStatus(GameStatus status) { this.status = status; }
    public Disc getNextPlayer() { return nextPlayer; }
    public Disc getWinner() { return winner; }
    public void setWinner(Disc winner) { this.winner = winner; }
}