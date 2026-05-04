package com.portfolio.tetris;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.Window;

public class MultiplayerPanel extends JPanel implements KeyListener, ActionListener {

    private static final int CELL    = 24;  // 멀티플레이 셀 크기 (싱글보다 작게)
    private static final int BOARD_W = TetrisBoard.COLS * CELL;
    private static final int BOARD_H = TetrisBoard.ROWS * CELL;
    private static final int SIDE_W  = 110;
    private static final int GAP     = 20;

    private static final Color BG       = new Color(0x1A1A2E);
    private static final Color GRID     = new Color(0x2A2A4A);
    private static final Color BORDER   = new Color(0x4A4A8A);
    private static final Color PANEL_BG = new Color(0x16213E);
    private static final Color GARBAGE  = new Color(0x555555);

    private final TetrisBoard myBoard;
    private String[][] opponentBoard = new String[TetrisBoard.ROWS][TetrisBoard.COLS];

    private final NetworkManager network;
    private int myNumber = 0;
    private String status = "상대방 기다리는 중...";
    private boolean gameStarted = false;
    private boolean myGameOver  = false;
    private boolean opGameOver  = false;
    private boolean resultShown = false;
    private int opScore = 0, opLevel = 1, opLines = 0;

    private Timer gameTimer;
    private Timer networkTimer;
    private int incomingAttack = 0;

    // 재연결 콜백 (Main.java에서 설정)
    private Runnable onReconnect;

    public void setOnReconnect(Runnable callback) { this.onReconnect = callback; }

    public MultiplayerPanel(NetworkManager network) {
        this.network = network;
        this.myBoard = new TetrisBoard();

        int totalW = SIDE_W + BOARD_W + GAP + BOARD_W + SIDE_W;
        setPreferredSize(new Dimension(totalW, BOARD_H));
        setBackground(BG);
        setFocusable(true);
        addKeyListener(this);

        network.setOnMessage(this::handleMessage);
        network.setOnDisconnect(() -> SwingUtilities.invokeLater(() -> {
            status = "상대방 연결 끊김";
            stopTimers();
            repaint();
        }));
    }

    // 서버로부터 메시지 처리
    private void handleMessage(String msg) {
        SwingUtilities.invokeLater(() -> {
            if (msg.startsWith("START:")) {
                myNumber = Integer.parseInt(msg.substring(6));
                status = "게임 시작! 나는 Player " + myNumber;
                gameStarted = true;
                startTimers();

            } else if (msg.startsWith("BOARD:")) {
                parseOpponentBoard(msg.substring(6));

            } else if (msg.startsWith("SCORE:")) {
                String[] parts = msg.substring(6).split(",");
                opScore  = Integer.parseInt(parts[0]);
                opLevel  = Integer.parseInt(parts[1]);
                opLines  = Integer.parseInt(parts[2]);

            } else if (msg.startsWith("ATTACK:")) {
                int lines = Integer.parseInt(msg.substring(7));
                incomingAttack = lines;
                myBoard.addGarbageLines(lines);

            } else if (msg.equals("GAMEOVER")) {
                opGameOver = true;
                status = "승리! 상대방이 게임오버!";
                stopTimers();
                showResultDialog("승리!", "상대방이 게임오버 되었습니다.");

            } else if (msg.equals("DISCONNECT")) {
                status = "상대방 연결 끊김";
                stopTimers();
                showResultDialog("연결 끊김", "상대방이 연결을 끊었습니다.");
            }
            repaint();
        });
    }

    private void startTimers() {
        // 게임 루프
        gameTimer = new Timer(myBoard.getSpeed(), this);
        gameTimer.start();

        // 네트워크 전송 (200ms마다)
        networkTimer = new Timer(200, e -> {
            if (!gameStarted || myGameOver) return;
            network.send("BOARD:" + myBoard.serializeBoard());
            network.send("SCORE:" + myBoard.getScore() + ","
                    + myBoard.getLevel() + "," + myBoard.getLinesCleared());
            int atk = myBoard.getAndResetAttack();
            if (atk > 0) network.send("ATTACK:" + atk);
        });
        networkTimer.start();
    }

    private void stopTimers() {
        if (gameTimer   != null) gameTimer.stop();
        if (networkTimer != null) networkTimer.stop();
    }

    private void showResultDialog(String title, String msg) {
        if (resultShown) return;
        resultShown = true;
        JOptionPane pane = new JOptionPane(
                msg + "\n\n재접속하시겠습니까?",
                JOptionPane.QUESTION_MESSAGE,
                JOptionPane.YES_NO_OPTION,
                null,
                new String[]{"재접속", "나가기"}, "재접속");
        JDialog dlg = pane.createDialog(this, title);
        dlg.setVisible(true);
        Object val = pane.getValue();
        if ("재접속".equals(val)) {
            if (onReconnect != null) onReconnect.run();
        } else {
            Window w = SwingUtilities.getWindowAncestor(this);
            if (w != null) w.dispose();
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!gameStarted || myGameOver) return;
        myBoard.moveDown();
        if (myBoard.isGameOver()) {
            myGameOver = true;
            status = "게임 오버... 상대방 승리";
            network.send("GAMEOVER");
            stopTimers();
            showResultDialog("게임 오버", "상대방이 승리했습니다.");
        }
        if (gameTimer.getDelay() != myBoard.getSpeed())
            gameTimer.setDelay(myBoard.getSpeed());
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (!gameStarted) {
            drawWaiting(g2);
            return;
        }

        // 내 보드 (왼쪽)
        int myX = SIDE_W;
        drawMyBoard(g2, myX);
        drawMyInfo(g2, 0, myX);

        // 구분선
        int mid = myX + BOARD_W + GAP / 2;
        g2.setColor(new Color(0x3A3A6A));
        g2.setStroke(new BasicStroke(2));
        g2.drawLine(mid, 20, mid, BOARD_H - 20);

        // 상대 보드 (오른쪽)
        int opX = myX + BOARD_W + GAP;
        drawOpponentBoard(g2, opX);
        drawOpInfo(g2, opX + BOARD_W, opX);

        // 상태 메시지
        if (myGameOver || opGameOver) {
            drawOverlay(g2);
        }

        // 공격 라인 알림
        if (incomingAttack > 0) {
            drawAttackAlert(g2, myX);
            incomingAttack = 0;
        }
    }

    private void drawMyBoard(Graphics2D g, int startX) {
        // 배경
        g.setColor(BG);
        g.fillRect(startX, 0, BOARD_W, BOARD_H);

        // 고스트 블록
        TetrisBlock block = myBoard.getCurrentBlock();
        int ghostY = myBoard.getGhostY();
        if (ghostY != block.y) {
            int[][] shape = block.getShape();
            Color gc = block.getColor();
            g.setColor(new Color(gc.getRed(), gc.getGreen(), gc.getBlue(), 50));
            for (int r = 0; r < 4; r++)
                for (int c = 0; c < 4; c++)
                    if (shape[r][c] == 1)
                        g.fillRoundRect(startX + (block.x+c)*CELL+1, (ghostY+r)*CELL+1, CELL-2, CELL-2, 4, 4);
        }

        // 보드 셀
        Color[][] board = myBoard.getBoard();
        for (int r = 0; r < TetrisBoard.ROWS; r++)
            for (int c = 0; c < TetrisBoard.COLS; c++)
                if (board[r][c] != null)
                    drawCell(g, startX + c*CELL, r*CELL, board[r][c]);

        // 현재 블록
        int[][] shape = block.getShape();
        for (int r = 0; r < 4; r++)
            for (int c = 0; c < 4; c++)
                if (shape[r][c] == 1)
                    drawCell(g, startX + (block.x+c)*CELL, (block.y+r)*CELL, block.getColor());

        // 그리드
        drawGrid(g, startX);

        // 일시정지 오버레이
        if (myBoard.isPaused()) {
            g.setColor(new Color(0,0,0,150));
            g.fillRect(startX, 0, BOARD_W, BOARD_H);
            g.setFont(new Font("Malgun Gothic", Font.BOLD, 20));
            g.setColor(Color.WHITE);
            g.drawString("PAUSED", startX + BOARD_W/2 - 40, BOARD_H/2);
        }
    }

    private void drawOpponentBoard(Graphics2D g, int startX) {
        g.setColor(new Color(0x12122A));
        g.fillRect(startX, 0, BOARD_W, BOARD_H);

        for (int r = 0; r < TetrisBoard.ROWS; r++)
            for (int c = 0; c < TetrisBoard.COLS; c++) {
                String val = opponentBoard[r][c];
                if (val != null && !val.equals("0")) {
                    Color color = indexToColor(val);
                    drawCell(g, startX + c*CELL, r*CELL, color);
                }
            }

        drawGrid(g, startX);

        if (opGameOver) {
            g.setColor(new Color(0,0,0,150));
            g.fillRect(startX, 0, BOARD_W, BOARD_H);
            g.setFont(new Font("Malgun Gothic", Font.BOLD, 18));
            g.setColor(new Color(0xFF4444));
            FontMetrics fm = g.getFontMetrics();
            String txt = "GAME OVER";
            g.drawString(txt, startX + (BOARD_W - fm.stringWidth(txt))/2, BOARD_H/2);
        }
    }

    private void drawMyInfo(Graphics2D g, int panelStartX, int boardStartX) {
        g.setColor(PANEL_BG);
        g.fillRect(panelStartX, 0, SIDE_W, BOARD_H);
        int x = panelStartX + 8;

        drawInfoBlock(g, x, 20,  "ME", "Player " + myNumber, Color.WHITE);
        drawInfoBlock(g, x, 90,  "SCORE", String.valueOf(myBoard.getScore()), Color.WHITE);
        drawInfoBlock(g, x, 160, "LEVEL", String.valueOf(myBoard.getLevel()), new Color(0xFFD700));
        drawInfoBlock(g, x, 230, "LINES", String.valueOf(myBoard.getLinesCleared()), new Color(0x7DD87A));

        // HOLD
        g.setFont(new Font("Malgun Gothic", Font.BOLD, 11));
        g.setColor(new Color(0xA0A0C0));
        g.drawString("HOLD [C]", x, 310);
        TetrisBlock held = myBoard.getHeldBlock();
        if (held != null) drawMiniBlock(g, held, x, 320);

        // NEXT
        g.setFont(new Font("Malgun Gothic", Font.BOLD, 11));
        g.setColor(new Color(0xA0A0C0));
        g.drawString("NEXT", x, 420);
        drawMiniBlock(g, myBoard.getNextBlock(), x, 430);

        // 키 가이드
        g.setFont(new Font("Malgun Gothic", Font.PLAIN, 9));
        g.setColor(new Color(0x505070));
        String[] keys = {"←→ Move","↑ Rotate","↓ Drop","SPACE Hard","C Hold","P Pause"};
        int ky = BOARD_H - 75;
        for (String k : keys) { g.drawString(k, x, ky); ky += 13; }
    }

    private void drawOpInfo(Graphics2D g, int panelStartX, int boardStartX) {
        g.setColor(PANEL_BG);
        g.fillRect(panelStartX, 0, SIDE_W, BOARD_H);
        int x = panelStartX + 8;
        int opNum = myNumber == 1 ? 2 : 1;

        drawInfoBlock(g, x, 20,  "OPPONENT", "Player " + opNum, new Color(0xFF6B6B));
        drawInfoBlock(g, x, 90,  "SCORE", String.valueOf(opScore),  new Color(0xFF6B6B));
        drawInfoBlock(g, x, 160, "LEVEL", String.valueOf(opLevel),  new Color(0xFFD700));
        drawInfoBlock(g, x, 230, "LINES", String.valueOf(opLines),  new Color(0x7DD87A));
    }

    private void drawInfoBlock(Graphics2D g, int x, int y, String label, String value, Color valueColor) {
        g.setFont(new Font("Malgun Gothic", Font.PLAIN, 11));
        g.setColor(new Color(0xA0A0C0));
        g.drawString(label, x, y);
        g.setFont(new Font("Malgun Gothic", Font.BOLD, 18));
        g.setColor(valueColor);
        g.drawString(value, x, y + 22);
    }

    private void drawMiniBlock(Graphics2D g, TetrisBlock block, int startX, int startY) {
        int mini = 16;
        int[][] shape = block.getShape();
        for (int r = 0; r < 4; r++)
            for (int c = 0; c < 4; c++)
                if (shape[r][c] == 1) {
                    g.setColor(block.getColor());
                    g.fillRoundRect(startX + c*mini + 1, startY + r*mini + 1, mini-2, mini-2, 4, 4);
                    g.setColor(new Color(255,255,255,60));
                    g.fillRoundRect(startX + c*mini + 2, startY + r*mini + 2, mini-4, mini/3, 3, 3);
                }
    }

    private void drawCell(Graphics2D g, int x, int y, Color color) {
        g.setColor(color);
        g.fillRoundRect(x+1, y+1, CELL-2, CELL-2, 4, 4);
        g.setColor(new Color(255,255,255,60));
        g.fillRoundRect(x+2, y+2, CELL-4, CELL/3, 3, 3);
    }

    private void drawGrid(Graphics2D g, int startX) {
        g.setStroke(new BasicStroke(0.5f));
        g.setColor(GRID);
        for (int r = 0; r <= TetrisBoard.ROWS; r++)
            g.drawLine(startX, r*CELL, startX+BOARD_W, r*CELL);
        for (int c = 0; c <= TetrisBoard.COLS; c++)
            g.drawLine(startX+c*CELL, 0, startX+c*CELL, BOARD_H);
        g.setStroke(new BasicStroke(2f));
        g.setColor(BORDER);
        g.drawRect(startX, 0, BOARD_W, BOARD_H);
    }

    private void drawWaiting(Graphics2D g) {
        g.setFont(new Font("Malgun Gothic", Font.BOLD, 22));
        g.setColor(Color.WHITE);
        FontMetrics fm = g.getFontMetrics();
        int w = getWidth(), h = getHeight();
        g.drawString(status, (w - fm.stringWidth(status))/2, h/2 - 20);
        g.setFont(new Font("Malgun Gothic", Font.PLAIN, 14));
        g.setColor(new Color(0xA0A0C0));
        String sub = "서버에 2명이 접속하면 자동 시작됩니다.";
        fm = g.getFontMetrics();
        g.drawString(sub, (w - fm.stringWidth(sub))/2, h/2 + 20);
    }

    private void drawOverlay(Graphics2D g) {
        g.setColor(new Color(0,0,0,160));
        g.fillRect(0, 0, getWidth(), BOARD_H);
        g.setFont(new Font("Malgun Gothic", Font.BOLD, 28));
        FontMetrics fm = g.getFontMetrics();
        g.setColor(opGameOver ? new Color(0xFFD700) : new Color(0xFF4444));
        g.drawString(status, (getWidth() - fm.stringWidth(status))/2, BOARD_H/2);
        g.setFont(new Font("Malgun Gothic", Font.PLAIN, 14));
        g.setColor(new Color(0xA0A0C0));
        String sub = "R 키로 다시 접속";
        fm = g.getFontMetrics();
        g.drawString(sub, (getWidth() - fm.stringWidth(sub))/2, BOARD_H/2 + 35);
    }

    private void drawAttackAlert(Graphics2D g, int myBoardX) {
        g.setColor(new Color(255, 50, 50, 200));
        g.setFont(new Font("Malgun Gothic", Font.BOLD, 16));
        g.drawString("ATTACK +" + incomingAttack, myBoardX + 10, 30);
    }

    private void parseOpponentBoard(String data) {
        if (data.length() < TetrisBoard.ROWS * TetrisBoard.COLS) return;
        int idx = 0;
        for (int r = 0; r < TetrisBoard.ROWS; r++)
            for (int c = 0; c < TetrisBoard.COLS; c++)
                opponentBoard[r][c] = String.valueOf(data.charAt(idx++));
    }

    private Color indexToColor(String idx) {
        try {
            int i = Integer.parseInt(idx) - 1;
            if (i < 0) return null;
            if (i < TetrisBlock.COLORS.length) return TetrisBlock.COLORS[i];
            return GARBAGE;
        } catch (NumberFormatException e) { return null; }
    }

    // 키보드 입력
    @Override
    public void keyPressed(KeyEvent e) {
        // R키 - 게임오버 여부 관계없이 재접속 다이얼로그
        if (e.getKeyCode() == KeyEvent.VK_R) {
            stopTimers();
            showResultDialog("재접속", "재접속하시겠습니까?");
            return;
        }
        if (!gameStarted || myGameOver) return;
        switch (e.getKeyCode()) {
            case KeyEvent.VK_LEFT:  myBoard.moveLeft();   break;
            case KeyEvent.VK_RIGHT: myBoard.moveRight();  break;
            case KeyEvent.VK_UP:    myBoard.rotate();     break;
            case KeyEvent.VK_DOWN:  myBoard.moveDown();   break;
            case KeyEvent.VK_SPACE: myBoard.hardDrop();   break;
            case KeyEvent.VK_C:     myBoard.holdBlock();  break;
            case KeyEvent.VK_P:
                myBoard.togglePause();
                if (!myBoard.isPaused()) {
                    gameTimer.setDelay(myBoard.getSpeed());
                    gameTimer.restart();
                } else { gameTimer.stop(); }
                break;
        }
        repaint();
    }

    @Override public void keyReleased(KeyEvent e) {}
    @Override public void keyTyped(KeyEvent e) {}
}
