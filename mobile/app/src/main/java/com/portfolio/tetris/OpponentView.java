package com.portfolio.tetris;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

public class OpponentView extends View {

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private int[][] board = new int[TetrisBoard.ROWS][TetrisBoard.COLS]; // 색상 인덱스
    private float cellSize;
    private boolean gameOver = false;

    public OpponentView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldW, int oldH) {
        super.onSizeChanged(w, h, oldW, oldH);
        cellSize = Math.min((float)w / TetrisBoard.COLS, (float)h / TetrisBoard.ROWS);
    }

    public void updateBoard(String boardData) {
        if (boardData.length() < TetrisBoard.ROWS * TetrisBoard.COLS) return;
        int idx = 0;
        for (int r = 0; r < TetrisBoard.ROWS; r++)
            for (int c = 0; c < TetrisBoard.COLS; c++)
                board[r][c] = boardData.charAt(idx++) - '0';
        invalidate();
    }

    public void setGameOver(boolean gameOver) {
        this.gameOver = gameOver;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(Color.parseColor("#12122A"));

        // 보드 그리기
        for (int r = 0; r < TetrisBoard.ROWS; r++) {
            for (int c = 0; c < TetrisBoard.COLS; c++) {
                int idx = board[r][c];
                if (idx > 0) {
                    int color = idx <= TetrisBlock.COLORS.length
                            ? TetrisBlock.COLORS[idx - 1]
                            : Color.parseColor("#555555");
                    float l = c * cellSize + 1;
                    float t = r * cellSize + 1;
                    paint.setColor(color);
                    paint.setAlpha(255);
                    paint.setStyle(Paint.Style.FILL);
                    canvas.drawRoundRect(new RectF(l, t, l + cellSize - 2, t + cellSize - 2), 4, 4, paint);
                    paint.setColor(Color.WHITE);
                    paint.setAlpha(50);
                    canvas.drawRoundRect(new RectF(l + 1, t + 1, l + cellSize - 3, t + cellSize * 0.35f), 3, 3, paint);
                    paint.setAlpha(255);
                }
            }
        }

        // 그리드
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.parseColor("#2A2A4A"));
        paint.setStrokeWidth(0.5f);
        for (int r = 0; r <= TetrisBoard.ROWS; r++)
            canvas.drawLine(0, r * cellSize, TetrisBoard.COLS * cellSize, r * cellSize, paint);
        for (int c = 0; c <= TetrisBoard.COLS; c++)
            canvas.drawLine(c * cellSize, 0, c * cellSize, TetrisBoard.ROWS * cellSize, paint);
        paint.setColor(Color.parseColor("#4A4A8A"));
        paint.setStrokeWidth(2f);
        canvas.drawRect(0, 0, TetrisBoard.COLS * cellSize, TetrisBoard.ROWS * cellSize, paint);

        // 게임오버 오버레이
        if (gameOver) {
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.parseColor("#AA000000"));
            canvas.drawRect(0, 0, getWidth(), getHeight(), paint);
            paint.setColor(Color.parseColor("#FF4444"));
            paint.setTextSize(cellSize * 0.9f);
            paint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("GAME", getWidth() / 2f, getHeight() / 2f - cellSize, paint);
            canvas.drawText("OVER", getWidth() / 2f, getHeight() / 2f + cellSize * 0.2f, paint);
            paint.setTextAlign(Paint.Align.LEFT);
        }
    }
}
