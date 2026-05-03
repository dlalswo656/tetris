package com.portfolio.tetris;

import java.awt.Color;

public class TetrisBoard {

    public static final int COLS = 10, ROWS = 20;
    private Color[][] board = new Color[ROWS][COLS];
    private TetrisBlock currentBlock, nextBlock;
    private int score = 0, level = 1, linesCleared = 0;
    private boolean gameOver = false, paused = false;
    private static final int[] SCORE_TABLE = {0, 100, 300, 500, 800};

    public TetrisBoard() {
        nextBlock = new TetrisBlock(TetrisBlock.randomType());
        spawnBlock();
    }

    private void spawnBlock() {
        currentBlock = nextBlock;
        nextBlock = new TetrisBlock(TetrisBlock.randomType());
        currentBlock.x = 3; currentBlock.y = 0;
        if (!isValid(currentBlock, 0, 0)) gameOver = true;
    }

    private boolean isValid(TetrisBlock b, int dx, int dy) {
        int[][] shape = b.getShape();
        for (int r = 0; r < 4; r++)
            for (int c = 0; c < 4; c++)
                if (shape[r][c] == 1) {
                    int nx = b.x + c + dx, ny = b.y + r + dy;
                    if (nx < 0 || nx >= COLS || ny >= ROWS) return false;
                    if (ny >= 0 && board[ny][nx] != null) return false;
                }
        return true;
    }

    public void moveLeft()  { if (!gameOver && !paused && isValid(currentBlock,-1,0)) currentBlock.x--; }
    public void moveRight() { if (!gameOver && !paused && isValid(currentBlock, 1,0)) currentBlock.x++; }

    public boolean moveDown() {
        if (gameOver || paused) return false;
        if (isValid(currentBlock, 0, 1)) { currentBlock.y++; return true; }
        lockBlock(); return false;
    }

    public void hardDrop() {
        if (gameOver || paused) return;
        while (isValid(currentBlock, 0, 1)) { currentBlock.y++; score += 2; }
        lockBlock();
    }

    public void rotate() {
        if (gameOver || paused) return;
        currentBlock.rotate();
        if (!isValid(currentBlock, 0, 0)) {
            if      (isValid(currentBlock,  1, 0)) currentBlock.x++;
            else if (isValid(currentBlock, -1, 0)) currentBlock.x--;
            else if (isValid(currentBlock,  2, 0)) currentBlock.x += 2;
            else currentBlock.rotateBack();
        }
    }

    private void lockBlock() {
        int[][] shape = currentBlock.getShape();
        for (int r = 0; r < 4; r++)
            for (int c = 0; c < 4; c++)
                if (shape[r][c] == 1) {
                    int x = currentBlock.x + c, y = currentBlock.y + r;
                    if (y >= 0 && y < ROWS && x >= 0 && x < COLS)
                        board[y][x] = currentBlock.getColor();
                }
        clearLines();
        spawnBlock();
    }

    private void clearLines() {
        int cleared = 0;
        for (int r = ROWS - 1; r >= 0; r--)
            if (isLineFull(r)) { removeLine(r); r++; cleared++; }
        if (cleared > 0) {
            score += SCORE_TABLE[Math.min(cleared, 4)] * level;
            linesCleared += cleared;
            level = linesCleared / 10 + 1;
        }
    }

    private boolean isLineFull(int row) {
        for (int c = 0; c < COLS; c++) if (board[row][c] == null) return false;
        return true;
    }

    private void removeLine(int row) {
        for (int r = row; r > 0; r--) board[r] = board[r-1].clone();
        board[0] = new Color[COLS];
    }

    public int getGhostY() {
        int gy = currentBlock.y;
        while (isValidAt(currentBlock, currentBlock.x, gy + 1)) gy++;
        return gy;
    }

    private boolean isValidAt(TetrisBlock b, int px, int py) {
        int[][] shape = b.getShape();
        for (int r = 0; r < 4; r++)
            for (int c = 0; c < 4; c++)
                if (shape[r][c] == 1) {
                    int nx = px + c, ny = py + r;
                    if (nx < 0 || nx >= COLS || ny >= ROWS) return false;
                    if (ny >= 0 && board[ny][nx] != null) return false;
                }
        return true;
    }

    public int getSpeed() { return Math.max(100, 800 - (level - 1) * 70); }

    public void togglePause() { paused = !paused; }
    public void restart() {
        board = new Color[ROWS][COLS];
        score = 0; level = 1; linesCleared = 0; gameOver = false; paused = false;
        nextBlock = new TetrisBlock(TetrisBlock.randomType());
        spawnBlock();
    }

    public Color[][] getBoard()        { return board; }
    public TetrisBlock getCurrentBlock(){ return currentBlock; }
    public TetrisBlock getNextBlock()   { return nextBlock; }
    public int getScore()              { return score; }
    public int getLevel()              { return level; }
    public int getLinesCleared()       { return linesCleared; }
    public boolean isGameOver()        { return gameOver; }
    public boolean isPaused()          { return paused; }
}
