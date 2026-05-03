package com.portfolio.tetris;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class TetrisView extends View {

    private TetrisBoard board;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Handler handler = new Handler(Looper.getMainLooper());
    private float cellSize;
    private float offsetX, offsetY;

    // 터치 관련
    private float touchStartX, touchStartY;
    private static final float SWIPE_THRESHOLD = 40f;
    private boolean touchMoved = false;

    // 게임 루프 - 버그수정: 루프가 중복 실행되지 않도록 flag 추가
    private boolean loopRunning = false;
    private final Runnable gameLoop = new Runnable() {
        @Override
        public void run() {
            if (!loopRunning) return;
            if (!board.isGameOver() && !board.isPaused()) {
                board.moveDown();
                invalidate();
                handler.postDelayed(this, board.getSpeed());
            } else {
                // 게임오버 or 일시정지면 루프 정지
                loopRunning = false;
                invalidate();
            }
        }
    };

    public TetrisView(Context context, AttributeSet attrs) {
        super(context, attrs);
        board = new TetrisBoard();
        // 키보드 입력 받기 위해 포커스 설정
        setFocusable(true);
        setFocusableInTouchMode(true);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldW, int oldH) {
        super.onSizeChanged(w, h, oldW, oldH);
        float boardWidth = w * 0.68f;
        cellSize = boardWidth / TetrisBoard.COLS;
        offsetX = 0;
        offsetY = (h - cellSize * TetrisBoard.ROWS) / 2f;
        startGameLoop();
    }

    public void startGameLoop() {
        handler.removeCallbacks(gameLoop);
        loopRunning = true;
        handler.post(gameLoop);
    }

    public void resumeLoop() {
        if (!loopRunning && !board.isGameOver() && !board.isPaused()) {
            loopRunning = true;
            handler.post(gameLoop);
        }
    }

    public void restart() {
        handler.removeCallbacks(gameLoop);
        loopRunning = false;
        board.restart();
        startGameLoop();
        invalidate();
    }

    public void togglePause() {
        board.togglePause();
        if (!board.isPaused()) {
            // 일시정지 해제 시 루프 재시작
            startGameLoop();
        }
        invalidate();
    }

    public void pauseGame() {
        if (!board.isPaused()) {
            board.togglePause();
            loopRunning = false;
        }
    }

    public TetrisBoard getBoard() { return board; }

    // 키보드 입력 처리 (에뮬레이터 PC 키보드)
    public void handleLeft()      { board.moveLeft();  invalidate(); }
    public void handleRight()     { board.moveRight(); invalidate(); }
    public void handleDown()      { board.moveDown();  invalidate(); }
    public void handleRotate()    { board.rotate();    invalidate(); }
    public void handleHardDrop()  { board.hardDrop();  invalidate(); }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(Color.parseColor("#1A1A2E"));

        drawBoard(canvas);
        drawGhost(canvas);
        drawCurrentBlock(canvas);
        drawGrid(canvas);
        drawSidePanel(canvas);

        if (board.isGameOver())   drawGameOver(canvas);
        else if (board.isPaused()) drawPaused(canvas);
    }

    private void drawBoard(Canvas canvas) {
        int[][] b = board.getBoard();
        for (int r = 0; r < TetrisBoard.ROWS; r++)
            for (int c = 0; c < TetrisBoard.COLS; c++)
                if (b[r][c] != 0) drawCell(canvas, c, r, b[r][c]);
    }

    private void drawGhost(Canvas canvas) {
        TetrisBlock block = board.getCurrentBlock();
        int ghostY = board.getGhostY();
        if (ghostY == block.y) return;
        int[][] shape = block.getShape();
        paint.setColor(block.getColor());
        paint.setAlpha(50);
        paint.setStyle(Paint.Style.FILL);
        for (int r = 0; r < 4; r++)
            for (int c = 0; c < 4; c++)
                if (shape[r][c] == 1) {
                    float l = offsetX + (block.x + c) * cellSize + 1;
                    float t = offsetY + (ghostY + r) * cellSize + 1;
                    canvas.drawRoundRect(new RectF(l, t, l + cellSize - 2, t + cellSize - 2), 4, 4, paint);
                }
        paint.setAlpha(255);
    }

    private void drawCurrentBlock(Canvas canvas) {
        TetrisBlock block = board.getCurrentBlock();
        int[][] shape = block.getShape();
        for (int r = 0; r < 4; r++)
            for (int c = 0; c < 4; c++)
                if (shape[r][c] == 1)
                    drawCell(canvas, block.x + c, block.y + r, block.getColor());
    }

    private void drawCell(Canvas canvas, int col, int row, int color) {
        float left  = offsetX + col * cellSize + 1;
        float top   = offsetY + row * cellSize + 1;
        float right = left + cellSize - 2;
        float bot   = top + cellSize - 2;

        paint.setColor(color);
        paint.setAlpha(255);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawRoundRect(new RectF(left, top, right, bot), 6, 6, paint);

        // 하이라이트
        paint.setColor(Color.WHITE);
        paint.setAlpha(60);
        canvas.drawRoundRect(new RectF(left + 2, top + 2, right - 2, top + cellSize * 0.35f), 4, 4, paint);
        paint.setAlpha(255);
    }

    private void drawGrid(Canvas canvas) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.parseColor("#2A2A4A"));
        paint.setStrokeWidth(0.5f);
        for (int r = 0; r <= TetrisBoard.ROWS; r++)
            canvas.drawLine(offsetX, offsetY + r * cellSize,
                    offsetX + TetrisBoard.COLS * cellSize, offsetY + r * cellSize, paint);
        for (int c = 0; c <= TetrisBoard.COLS; c++)
            canvas.drawLine(offsetX + c * cellSize, offsetY,
                    offsetX + c * cellSize, offsetY + TetrisBoard.ROWS * cellSize, paint);
        paint.setColor(Color.parseColor("#4A4A8A"));
        paint.setStrokeWidth(2f);
        canvas.drawRect(offsetX, offsetY,
                offsetX + TetrisBoard.COLS * cellSize,
                offsetY + TetrisBoard.ROWS * cellSize, paint);
    }

    private void drawSidePanel(Canvas canvas) {
        float panelX = offsetX + TetrisBoard.COLS * cellSize + 16;
        float mini = cellSize * 0.6f;
        paint.setStyle(Paint.Style.FILL);
        paint.setTextAlign(Paint.Align.LEFT);

        // ── HOLD ──
        paint.setTextSize(cellSize * 0.5f);
        paint.setColor(Color.parseColor("#A0A0C0"));
        canvas.drawText("HOLD", panelX, offsetY + cellSize * 1.0f, paint);

        // 홀드 박스 배경
        paint.setColor(Color.parseColor("#2A2A4A"));
        canvas.drawRoundRect(new RectF(panelX - 4, offsetY + cellSize * 1.1f,
                panelX + mini * 4 + 4, offsetY + cellSize * 1.1f + mini * 4 + 4), 6, 6, paint);

        TetrisBlock held = board.getHeldBlock();
        if (held != null) {
            int[][] hShape = held.getShape();
            paint.setAlpha(board.canHold() ? 255 : 100);
            for (int r = 0; r < 4; r++)
                for (int c = 0; c < 4; c++)
                    if (hShape[r][c] == 1) {
                        float l = panelX + c * mini;
                        float t = offsetY + cellSize * 1.2f + r * mini;
                        paint.setColor(held.getColor());
                        paint.setStyle(Paint.Style.FILL);
                        canvas.drawRoundRect(new RectF(l + 1, t + 1, l + mini - 1, t + mini - 1), 4, 4, paint);
                    }
            paint.setAlpha(255);
        }

        // ── SCORE ──
        paint.setTextSize(cellSize * 0.5f);
        paint.setColor(Color.parseColor("#A0A0C0"));
        canvas.drawText("SCORE", panelX, offsetY + cellSize * 5.0f, paint);
        paint.setColor(Color.WHITE);
        paint.setTextSize(cellSize * 0.65f);
        canvas.drawText(String.valueOf(board.getScore()), panelX, offsetY + cellSize * 5.9f, paint);

        // ── LEVEL ──
        paint.setTextSize(cellSize * 0.5f);
        paint.setColor(Color.parseColor("#A0A0C0"));
        canvas.drawText("LEVEL", panelX, offsetY + cellSize * 7.1f, paint);
        paint.setColor(Color.parseColor("#FFD700"));
        paint.setTextSize(cellSize * 0.65f);
        canvas.drawText(String.valueOf(board.getLevel()), panelX, offsetY + cellSize * 8.0f, paint);

        // ── LINES ──
        paint.setTextSize(cellSize * 0.5f);
        paint.setColor(Color.parseColor("#A0A0C0"));
        canvas.drawText("LINES", panelX, offsetY + cellSize * 9.2f, paint);
        paint.setColor(Color.parseColor("#7DD87A"));
        paint.setTextSize(cellSize * 0.65f);
        canvas.drawText(String.valueOf(board.getLinesCleared()), panelX, offsetY + cellSize * 10.1f, paint);

        // ── NEXT ──
        paint.setTextSize(cellSize * 0.5f);
        paint.setColor(Color.parseColor("#A0A0C0"));
        canvas.drawText("NEXT", panelX, offsetY + cellSize * 11.5f, paint);
        TetrisBlock next = board.getNextBlock();
        int[][] shape = next.getShape();
        for (int r = 0; r < 4; r++)
            for (int c = 0; c < 4; c++)
                if (shape[r][c] == 1) {
                    float l = panelX + c * mini;
                    float t = offsetY + cellSize * 11.9f + r * mini;
                    paint.setColor(next.getColor());
                    paint.setStyle(Paint.Style.FILL);
                    canvas.drawRoundRect(new RectF(l + 1, t + 1, l + mini - 1, t + mini - 1), 4, 4, paint);
                }

        // ── 조작 안내 ──
        paint.setTextSize(cellSize * 0.35f);
        paint.setColor(Color.parseColor("#606080"));
        canvas.drawText("Tap: Rotate", panelX, offsetY + cellSize * 16.2f, paint);
        canvas.drawText("Swipe L/R: Move", panelX, offsetY + cellSize * 16.9f, paint);
        canvas.drawText("Swipe Down: Drop", panelX, offsetY + cellSize * 17.6f, paint);
        canvas.drawText("Swipe Up: Hold", panelX, offsetY + cellSize * 18.3f, paint);
    }

    private void drawGameOver(Canvas canvas) {
        paint.setColor(Color.parseColor("#CC000000"));
        paint.setStyle(Paint.Style.FILL);
        canvas.drawRect(0, 0, getWidth(), getHeight(), paint);
        paint.setColor(Color.WHITE);
        paint.setTextSize(cellSize * 1.1f);
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("GAME OVER", getWidth() / 2f, getHeight() / 2f - cellSize * 1.5f, paint);
        paint.setTextSize(cellSize * 0.65f);
        paint.setColor(Color.parseColor("#FFD700"));
        canvas.drawText("SCORE: " + board.getScore(), getWidth() / 2f, getHeight() / 2f, paint);
        paint.setTextSize(cellSize * 0.6f);
        paint.setColor(Color.parseColor("#A0A0C0"));
        canvas.drawText("탭하여 다시시작", getWidth() / 2f, getHeight() / 2f + cellSize * 1.5f, paint);
        paint.setTextAlign(Paint.Align.LEFT);
    }

    private void drawPaused(Canvas canvas) {
        paint.setColor(Color.parseColor("#AA000000"));
        paint.setStyle(Paint.Style.FILL);
        canvas.drawRect(0, 0, getWidth(), getHeight(), paint);
        paint.setColor(Color.WHITE);
        paint.setTextSize(cellSize * 1.1f);
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("PAUSED", getWidth() / 2f, getHeight() / 2f, paint);
        paint.setTextAlign(Paint.Align.LEFT);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                touchStartX = event.getX();
                touchStartY = event.getY();
                touchMoved = false;
                if (board.isGameOver()) { restart(); return true; }
                break;

            case MotionEvent.ACTION_MOVE:
                float mdx = event.getX() - touchStartX;
                float mdy = event.getY() - touchStartY;
                if (Math.abs(mdx) > SWIPE_THRESHOLD || Math.abs(mdy) > SWIPE_THRESHOLD)
                    touchMoved = true;
                break;

            case MotionEvent.ACTION_UP:
                float dx = event.getX() - touchStartX;
                float dy = event.getY() - touchStartY;
                if (!touchMoved) {
                    // 탭 → 회전
                    board.rotate();
                } else if (Math.abs(dx) > Math.abs(dy)) {
                    // 좌우 스와이프 → 이동
                    if (dx > SWIPE_THRESHOLD) board.moveRight();
                    else if (dx < -SWIPE_THRESHOLD) board.moveLeft();
                } else {
                    if (dy > SWIPE_THRESHOLD) board.hardDrop();       // 아래 스와이프 → 하드드롭
                    else if (dy < -SWIPE_THRESHOLD) board.holdBlock(); // 위 스와이프 → 홀드
                }
                invalidate();
                break;
        }
        requestFocus();
        return true;
    }
}
