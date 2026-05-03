package com.portfolio.tetris;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class TetrisPanel extends JPanel implements KeyListener, ActionListener {

    private static final int CELL   = 32;
    private static final int BOARD_W = TetrisBoard.COLS * CELL;
    private static final int BOARD_H = TetrisBoard.ROWS * CELL;
    private static final int SIDE_W  = 160;
    private static final Color BG     = new Color(0x1A1A2E);
    private static final Color GRID   = new Color(0x2A2A4A);
    private static final Color BORDER = new Color(0x4A4A8A);
    private static final Color PANEL_BG = new Color(0x16213E);
    private static final Color LABEL_COLOR = new Color(0xA0A0C0);

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

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!board.isGameOver() && !board.isPaused()) {
            board.moveDown();
            if (timer.getDelay() != board.getSpeed())
                timer.setDelay(board.getSpeed());
        }
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        drawBoard(g2);
        drawGhost(g2);
        drawCurrentBlock(g2);
        drawGrid(g2);
        drawSidePanel(g2);

        if (board.isGameOver())    drawOverlay(g2, "GAME OVER",  "SPACE / ENTER to restart");
        else if (board.isPaused()) drawOverlay(g2, "PAUSED",     "P to resume");
    }

    private void drawBoard(Graphics2D g) {
        Color[][] b = board.getBoard();
        for (int r = 0; r < TetrisBoard.ROWS; r++)
            for (int c = 0; c < TetrisBoard.COLS; c++)
                if (b[r][c] != null) drawCell(g, c, r, b[r][c]);
    }

    private void drawGhost(Graphics2D g) {
        TetrisBlock block = board.getCurrentBlock();
        int ghostY = board.getGhostY();
        if (ghostY == block.y) return;
        int[][] shape = block.getShape();
        Color gc = block.getColor();
        g.setColor(new Color(gc.getRed(), gc.getGreen(), gc.getBlue(), 50));
        for (int r = 0; r < 4; r++)
            for (int c = 0; c < 4; c++)
                if (shape[r][c] == 1)
                    g.fillRoundRect((block.x+c)*CELL+1, (ghostY+r)*CELL+1, CELL-2, CELL-2, 6, 6);
    }

    private void drawCurrentBlock(Graphics2D g) {
        TetrisBlock block = board.getCurrentBlock();
        int[][] shape = block.getShape();
        for (int r = 0; r < 4; r++)
            for (int c = 0; c < 4; c++)
                if (shape[r][c] == 1)
                    drawCell(g, block.x+c, block.y+r, block.getColor());
    }

    private void drawCell(Graphics2D g, int col, int row, Color color) {
        int x = col*CELL+1, y = row*CELL+1, sz = CELL-2;
        g.setColor(color);
        g.fillRoundRect(x, y, sz, sz, 6, 6);
        g.setColor(new Color(255,255,255,60));
        g.fillRoundRect(x+2, y+2, sz-4, sz/3, 4, 4);
    }

    private void drawMiniBlock(Graphics2D g, TetrisBlock block, int startX, int startY) {
        int[][] shape = block.getShape();
        int mini = 20;
        for (int r = 0; r < 4; r++)
            for (int c = 0; c < 4; c++)
                if (shape[r][c] == 1) {
                    int nx = startX + c*mini, ny = startY + r*mini;
                    g.setColor(block.getColor());
                    g.fillRoundRect(nx+1, ny+1, mini-2, mini-2, 4, 4);
                    g.setColor(new Color(255,255,255,60));
                    g.fillRoundRect(nx+2, ny+2, mini-4, mini/3, 3, 3);
                }
    }

    private void drawGrid(Graphics2D g) {
        g.setStroke(new BasicStroke(0.5f));
        g.setColor(GRID);
        for (int r = 0; r <= TetrisBoard.ROWS; r++)
            g.drawLine(0, r*CELL, BOARD_W, r*CELL);
        for (int c = 0; c <= TetrisBoard.COLS; c++)
            g.drawLine(c*CELL, 0, c*CELL, BOARD_H);
        g.setStroke(new BasicStroke(2f));
        g.setColor(BORDER);
        g.drawRect(0, 0, BOARD_W, BOARD_H);
    }

    private void drawSidePanel(Graphics2D g) {
        int px = BOARD_W + 12;

        // 패널 배경
        g.setColor(PANEL_BG);
        g.fillRect(BOARD_W, 0, SIDE_W, BOARD_H);

        // ── HOLD 박스 ───────────────────────────
        drawSectionBox(g, px, 10, "HOLD  [C]");
        TetrisBlock held = board.getHeldBlock();
        if (held != null) {
            // 홀드 불가 상태면 살짝 어둡게
            if (!board.canHold()) {
                g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.4f));
            }
            drawMiniBlock(g, held, px + 4, 36);
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
        } else {
            g.setFont(new Font("Arial", Font.PLAIN, 11));
            g.setColor(new Color(0x505070));
            g.drawString("None", px + 6, 58);
        }

        // ── NEXT 박스 ──────────────────────────
        drawSectionBox(g, px, 110, "NEXT");
        drawMiniBlock(g, board.getNextBlock(), px + 4, 134);

        // ── SCORE / LEVEL / LINES ──────────────
        int sy = 230;
        drawStatRow(g, px, sy,       "SCORE", String.valueOf(board.getScore()),    Color.WHITE);
        drawStatRow(g, px, sy + 70,  "LEVEL", String.valueOf(board.getLevel()),    new Color(0xFFD700));
        drawStatRow(g, px, sy + 140, "LINES", String.valueOf(board.getLinesCleared()), new Color(0x7DD87A));

        // ── 키 가이드 ──────────────────────────
        g.setFont(new Font("Arial", Font.PLAIN, 11));
        g.setColor(new Color(0x505070));
        int ky = BOARD_H - 125;
        String[] keys = {
            "LEFT / RIGHT : Move",
            "UP           : Rotate",
            "DOWN         : Soft drop",
            "SPACE        : Hard drop",
            "C            : Hold",
            "P            : Pause",
            "R            : Restart"
        };
        for (String k : keys) { g.drawString(k, px, ky); ky += 16; }
    }

    private void drawSectionBox(Graphics2D g, int x, int y, String title) {
        g.setColor(new Color(0x2A2A4A));
        g.fillRoundRect(x - 4, y, SIDE_W - 16, 90, 8, 8);
        g.setColor(BORDER);
        g.setStroke(new BasicStroke(1f));
        g.drawRoundRect(x - 4, y, SIDE_W - 16, 90, 8, 8);
        g.setFont(new Font("Arial", Font.BOLD, 12));
        g.setColor(LABEL_COLOR);
        g.drawString(title, x, y + 16);
    }

    private void drawStatRow(Graphics2D g, int x, int y, String label, String value, Color valueColor) {
        g.setFont(new Font("Arial", Font.PLAIN, 12));
        g.setColor(LABEL_COLOR);
        g.drawString(label, x, y);
        g.setFont(new Font("Arial", Font.BOLD, 22));
        g.setColor(valueColor);
        g.drawString(value, x, y + 26);
    }

    private void drawOverlay(Graphics2D g, String title, String sub) {
        g.setColor(new Color(0,0,0,180));
        g.fillRect(0, 0, BOARD_W, BOARD_H);

        g.setFont(new Font("Arial", Font.BOLD, 28));
        g.setColor(Color.WHITE);
        FontMetrics fm = g.getFontMetrics();
        g.drawString(title, (BOARD_W - fm.stringWidth(title))/2, BOARD_H/2 - 20);

        if (board.isGameOver()) {
            String score = "SCORE: " + board.getScore();
            g.setFont(new Font("Arial", Font.BOLD, 16));
            g.setColor(new Color(0xFFD700));
            fm = g.getFontMetrics();
            g.drawString(score, (BOARD_W - fm.stringWidth(score))/2, BOARD_H/2 + 12);
        }

        g.setFont(new Font("Arial", Font.PLAIN, 13));
        g.setColor(new Color(0xA0A0C0));
        fm = g.getFontMetrics();
        g.drawString(sub, (BOARD_W - fm.stringWidth(sub))/2, BOARD_H/2 + 40);
    }

    // ── 키보드 입력 ─────────────────────────────
    @Override
    public void keyPressed(KeyEvent e) {
        if (board.isGameOver()) {
            if (e.getKeyCode()==KeyEvent.VK_SPACE || e.getKeyCode()==KeyEvent.VK_ENTER) {
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
            case KeyEvent.VK_C:     board.holdBlock();  break; // 홀드
            case KeyEvent.VK_P:     board.togglePause(); break;
            case KeyEvent.VK_R:     board.restart(); startTimer(); break;
        }
        repaint();
    }

    @Override public void keyReleased(KeyEvent e) {}
    @Override public void keyTyped(KeyEvent e) {}
}
