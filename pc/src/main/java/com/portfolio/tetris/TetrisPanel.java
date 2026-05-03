package com.portfolio.tetris;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class TetrisPanel extends JPanel implements KeyListener, ActionListener {

    private static final int CELL = 32;
    private static final int BOARD_W = TetrisBoard.COLS * CELL;
    private static final int BOARD_H = TetrisBoard.ROWS * CELL;
    private static final int SIDE_W = 160;
    private static final Color BG   = new Color(0x1A1A2E);
    private static final Color GRID = new Color(0x2A2A4A);
    private static final Color BORDER = new Color(0x4A4A8A);

    private final TetrisBoard board;
    private Timer timer;

    public TetrisPanel() {
        board = new TetrisBoard();
        setPreferredSize(new Dimension(BOARD_W + SIDE_W, BOARD_H));
        setBackground(BG);
        setFocusable(true);
        addKeyListener(this);
        startTimer();
    }

    private void startTimer() {
        if (timer != null) timer.stop();
        timer = new Timer(board.getSpeed(), this);
        timer.start();
    }

    // 게임 루프 - Timer가 매 tick마다 호출
    @Override
    public void actionPerformed(ActionEvent e) {
        if (!board.isGameOver() && !board.isPaused()) {
            board.moveDown();
            // 레벨에 따라 타이머 속도 동적 조정
            if (timer.getDelay() != board.getSpeed()) {
                timer.setDelay(board.getSpeed());
            }
        }
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        drawBoard(g2);
        drawGhost(g2);
        drawCurrentBlock(g2);
        drawGrid(g2);
        drawSidePanel(g2);

        if (board.isGameOver()) drawOverlay(g2, "GAME OVER", "SPACE / ENTER 키로 재시작");
        else if (board.isPaused()) drawOverlay(g2, "PAUSED", "P 키로 재개");
    }

    private void drawBoard(Graphics2D g) {
        Color[][] b = board.getBoard();
        for (int r = 0; r < TetrisBoard.ROWS; r++)
            for (int c = 0; c < TetrisBoard.COLS; c++)
                if (b[r][c] != null) drawCell(g, c, r, b[r][c], 1.0f);
    }

    private void drawGhost(Graphics2D g) {
        TetrisBlock block = board.getCurrentBlock();
        int ghostY = board.getGhostY();
        if (ghostY == block.y) return;
        int[][] shape = block.getShape();
        Color ghostColor = new Color(
                block.getColor().getRed(),
                block.getColor().getGreen(),
                block.getColor().getBlue(), 50);
        g.setColor(ghostColor);
        for (int r = 0; r < 4; r++)
            for (int c = 0; c < 4; c++)
                if (shape[r][c] == 1)
                    g.fillRoundRect((block.x + c) * CELL + 1, (ghostY + r) * CELL + 1,
                            CELL - 2, CELL - 2, 6, 6);
    }

    private void drawCurrentBlock(Graphics2D g) {
        TetrisBlock block = board.getCurrentBlock();
        int[][] shape = block.getShape();
        for (int r = 0; r < 4; r++)
            for (int c = 0; c < 4; c++)
                if (shape[r][c] == 1)
                    drawCell(g, block.x + c, block.y + r, block.getColor(), 1.0f);
    }

    private void drawCell(Graphics2D g, int col, int row, Color color, float alpha) {
        int x = col * CELL + 1, y = row * CELL + 1;
        int sz = CELL - 2;
        // 메인 색
        g.setColor(alpha < 1f ? new Color(color.getRed(), color.getGreen(), color.getBlue(), (int)(255*alpha)) : color);
        g.fillRoundRect(x, y, sz, sz, 6, 6);
        // 하이라이트
        g.setColor(new Color(255, 255, 255, 60));
        g.fillRoundRect(x + 2, y + 2, sz - 4, sz / 3, 4, 4);
    }

    private void drawGrid(Graphics2D g) {
        g.setStroke(new BasicStroke(0.5f));
        g.setColor(GRID);
        for (int r = 0; r <= TetrisBoard.ROWS; r++)
            g.drawLine(0, r * CELL, BOARD_W, r * CELL);
        for (int c = 0; c <= TetrisBoard.COLS; c++)
            g.drawLine(c * CELL, 0, c * CELL, BOARD_H);
        g.setStroke(new BasicStroke(2f));
        g.setColor(BORDER);
        g.drawRect(0, 0, BOARD_W, BOARD_H);
    }

    private void drawSidePanel(Graphics2D g) {
        int px = BOARD_W + 16;
        g.setColor(new Color(0x16213E));
        g.fillRect(BOARD_W, 0, SIDE_W, BOARD_H);

        // SCORE
        drawLabel(g, px, 40, "SCORE", Color.WHITE, String.valueOf(board.getScore()), new Color(0xFFFFFF));
        // LEVEL
        drawLabel(g, px, 120, "LEVEL", new Color(0xA0A0C0), String.valueOf(board.getLevel()), new Color(0xFFD700));
        // LINES
        drawLabel(g, px, 200, "LINES", new Color(0xA0A0C0), String.valueOf(board.getLinesCleared()), new Color(0x7DD87A));

        // NEXT 블록
        g.setFont(new Font("Arial", Font.BOLD, 13));
        g.setColor(new Color(0xA0A0C0));
        g.drawString("NEXT", px, 290);

        TetrisBlock next = board.getNextBlock();
        int[][] shape = next.getShape();
        int mini = 20;
        for (int r = 0; r < 4; r++)
            for (int c = 0; c < 4; c++)
                if (shape[r][c] == 1) {
                    int nx = px + c * mini, ny = 300 + r * mini;
                    g.setColor(next.getColor());
                    g.fillRoundRect(nx + 1, ny + 1, mini - 2, mini - 2, 4, 4);
                    g.setColor(new Color(255, 255, 255, 60));
                    g.fillRoundRect(nx + 2, ny + 2, mini - 4, mini / 3, 3, 3);
                }

        // 키 조작 안내
        g.setFont(new Font("Arial", Font.PLAIN, 11));
        g.setColor(new Color(0x606080));
        String[] keys = {"← →  좌우 이동", "↑     회전", "↓     소프트드롭", "SPACE 하드드롭", "P     일시정지", "R     다시시작"};
        int ky = BOARD_H - 130;
        for (String k : keys) { g.drawString(k, px, ky); ky += 18; }
    }

    private void drawLabel(Graphics2D g, int x, int y, String label, Color labelColor, String value, Color valueColor) {
        g.setFont(new Font("Arial", Font.PLAIN, 13));
        g.setColor(labelColor);
        g.drawString(label, x, y);
        g.setFont(new Font("Arial", Font.BOLD, 22));
        g.setColor(valueColor);
        g.drawString(value, x, y + 28);
    }

    private void drawOverlay(Graphics2D g, String title, String sub) {
        g.setColor(new Color(0, 0, 0, 180));
        g.fillRect(0, 0, BOARD_W, BOARD_H);
        g.setFont(new Font("Arial", Font.BOLD, 28));
        g.setColor(Color.WHITE);
        FontMetrics fm = g.getFontMetrics();
        g.drawString(title, (BOARD_W - fm.stringWidth(title)) / 2, BOARD_H / 2 - 20);
        g.setFont(new Font("Arial", Font.PLAIN, 14));
        g.setColor(new Color(0xA0A0C0));
        fm = g.getFontMetrics();
        if (board.isGameOver()) {
            String score = "SCORE: " + board.getScore();
            g.setColor(new Color(0xFFD700));
            g.setFont(new Font("Arial", Font.BOLD, 16));
            fm = g.getFontMetrics();
            g.drawString(score, (BOARD_W - fm.stringWidth(score)) / 2, BOARD_H / 2 + 15);
            g.setFont(new Font("Arial", Font.PLAIN, 14));
            g.setColor(new Color(0xA0A0C0));
            fm = g.getFontMetrics();
        }
        g.drawString(sub, (BOARD_W - fm.stringWidth(sub)) / 2, BOARD_H / 2 + 45);
    }

    // ── 키보드 입력 처리 ──────────────────────────
    @Override
    public void keyPressed(KeyEvent e) {
        if (board.isGameOver()) {
            if (e.getKeyCode() == KeyEvent.VK_SPACE || e.getKeyCode() == KeyEvent.VK_ENTER) {
                board.restart(); startTimer();
            }
            return;
        }
        switch (e.getKeyCode()) {
            case KeyEvent.VK_LEFT:  board.moveLeft();   break;
            case KeyEvent.VK_RIGHT: board.moveRight();  break;
            case KeyEvent.VK_UP:    board.rotate();     break;
            case KeyEvent.VK_DOWN:  board.moveDown();   break;
            case KeyEvent.VK_SPACE: board.hardDrop();   break;
            case KeyEvent.VK_P:     board.togglePause();break;
            case KeyEvent.VK_R:     board.restart(); startTimer(); break;
        }
        repaint();
    }

    @Override public void keyReleased(KeyEvent e) {}
    @Override public void keyTyped(KeyEvent e) {}
}
