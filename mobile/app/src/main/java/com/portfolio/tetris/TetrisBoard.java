package com.portfolio.tetris;

public class TetrisBoard {

    public static final int COLS = 10;
    public static final int ROWS = 20;

    private int[][] board = new int[ROWS][COLS]; // 0 = 빈칸, 색상값 = 블록
    private TetrisBlock currentBlock;
    private TetrisBlock nextBlock;

    private int score = 0;
    private int level = 1;
    private int linesCleared = 0;
    private boolean gameOver = false;
    private boolean paused = false;

    // 점수 계산표
    private static final int[] SCORE_TABLE = {0, 100, 300, 500, 800};

    public TetrisBoard() {
        nextBlock = new TetrisBlock(TetrisBlock.randomType());
        spawnBlock();
    }

    private void spawnBlock() {
        currentBlock = nextBlock;
        nextBlock = new TetrisBlock(TetrisBlock.randomType());
        currentBlock.x = 3;
        currentBlock.y = 0;

        // 게임오버 체크 - 스폰 위치에 이미 블록이 있으면 게임오버
        if (!isValidPosition(currentBlock, 0, 0)) {
            gameOver = true;
        }
    }

    // 블록 이동 가능 여부 체크
    private boolean isValidPosition(TetrisBlock block, int dx, int dy) {
        int[][] shape = block.getShape();
        for (int r = 0; r < 4; r++) {
            for (int c = 0; c < 4; c++) {
                if (shape[r][c] == 1) {
                    int newX = block.x + c + dx;
                    int newY = block.y + r + dy;
                    if (newX < 0 || newX >= COLS || newY >= ROWS) return false;
                    if (newY >= 0 && board[newY][newX] != 0) return false;
                }
            }
        }
        return true;
    }

    // 블록 왼쪽 이동
    public void moveLeft() {
        if (!gameOver && !paused && isValidPosition(currentBlock, -1, 0))
            currentBlock.x--;
    }

    // 블록 오른쪽 이동
    public void moveRight() {
        if (!gameOver && !paused && isValidPosition(currentBlock, 1, 0))
            currentBlock.x++;
    }

    // 블록 아래 이동 (타이머/소프트드롭)
    public boolean moveDown() {
        if (gameOver || paused) return false;
        if (isValidPosition(currentBlock, 0, 1)) {
            currentBlock.y++;
            return true;
        } else {
            lockBlock();
            return false;
        }
    }

    // 블록 즉시 바닥으로 (하드드롭)
    public void hardDrop() {
        if (gameOver || paused) return;
        while (isValidPosition(currentBlock, 0, 1)) {
            currentBlock.y++;
            score += 2;
        }
        lockBlock();
    }

    // 블록 회전
    public void rotate() {
        if (gameOver || paused) return;
        currentBlock.rotate();
        // 벽킥: 회전 후 위치가 무효면 좌우로 조정 시도
        if (!isValidPosition(currentBlock, 0, 0)) {
            if (isValidPosition(currentBlock, 1, 0)) currentBlock.x++;
            else if (isValidPosition(currentBlock, -1, 0)) currentBlock.x--;
            else if (isValidPosition(currentBlock, 2, 0)) currentBlock.x += 2;
            else currentBlock.rotateBack(); // 회전 불가
        }
    }

    // 블록 고정
    private void lockBlock() {
        int[][] shape = currentBlock.getShape();
        for (int r = 0; r < 4; r++) {
            for (int c = 0; c < 4; c++) {
                if (shape[r][c] == 1) {
                    int x = currentBlock.x + c;
                    int y = currentBlock.y + r;
                    if (y >= 0 && y < ROWS && x >= 0 && x < COLS) {
                        board[y][x] = currentBlock.getColor();
                    }
                }
            }
        }
        clearLines();
        spawnBlock();
    }

    // 완성된 줄 삭제
    private void clearLines() {
        int cleared = 0;
        for (int r = ROWS - 1; r >= 0; r--) {
            if (isLineFull(r)) {
                removeLine(r);
                r++;
                cleared++;
            }
        }
        if (cleared > 0) {
            score += SCORE_TABLE[cleared] * level;
            linesCleared += cleared;
            level = linesCleared / 10 + 1;
        }
    }

    private boolean isLineFull(int row) {
        for (int c = 0; c < COLS; c++)
            if (board[row][c] == 0) return false;
        return true;
    }

    private void removeLine(int row) {
        for (int r = row; r > 0; r--)
            board[r] = board[r - 1].clone();
        board[0] = new int[COLS];
    }

    // 고스트 블록 y위치 계산 (블록이 떨어질 위치 미리보기)
    public int getGhostY() {
        int ghostY = currentBlock.y;
        while (isValidPositionAt(currentBlock, currentBlock.x, ghostY + 1))
            ghostY++;
        return ghostY;
    }

    private boolean isValidPositionAt(TetrisBlock block, int px, int py) {
        int[][] shape = block.getShape();
        for (int r = 0; r < 4; r++) {
            for (int c = 0; c < 4; c++) {
                if (shape[r][c] == 1) {
                    int nx = px + c;
                    int ny = py + r;
                    if (nx < 0 || nx >= COLS || ny >= ROWS) return false;
                    if (ny >= 0 && board[ny][nx] != 0) return false;
                }
            }
        }
        return true;
    }

    // 게임 속도 (ms) - 레벨 높을수록 빨라짐
    public int getSpeed() {
        return Math.max(100, 800 - (level - 1) * 70);
    }

    public void togglePause() { paused = !paused; }
    public void restart() {
        board = new int[ROWS][COLS];
        score = 0; level = 1; linesCleared = 0; gameOver = false; paused = false;
        nextBlock = new TetrisBlock(TetrisBlock.randomType());
        spawnBlock();
    }

    // Getter
    public int[][] getBoard() { return board; }
    public TetrisBlock getCurrentBlock() { return currentBlock; }
    public TetrisBlock getNextBlock() { return nextBlock; }
    public int getScore() { return score; }
    public int getLevel() { return level; }
    public int getLinesCleared() { return linesCleared; }
    public boolean isGameOver() { return gameOver; }
    public boolean isPaused() { return paused; }
}
