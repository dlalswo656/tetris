package com.portfolio.tetris;

import javax.swing.*;
import java.awt.*;

public class Main {

    public static void main(String[] args) {
        // 한글 폰트 전역 설정
        setKoreanFont();
        SwingUtilities.invokeLater(Main::showModeDialog);
    }

    private static void setKoreanFont() {
        Font korean = new Font("Malgun Gothic", Font.PLAIN, 13);
        String[] keys = {
            "Button.font", "Label.font", "TextField.font",
            "TextArea.font", "ComboBox.font", "OptionPane.messageFont",
            "OptionPane.buttonFont", "Dialog.font", "Panel.font"
        };
        for (String key : keys) UIManager.put(key, korean);
    }

    // ── 모드 선택 화면 ─────────────────────────────
    private static void showModeDialog() {
        JDialog dialog = new JDialog();
        dialog.setTitle("Tetris - Mode Select");
        dialog.setModal(true);
        dialog.setResizable(false);
        dialog.setLayout(new BorderLayout());

        dialog.setMinimumSize(new Dimension(400, 480));

        JPanel bg = new JPanel(new GridBagLayout());
        bg.setBackground(new Color(0x1A1A2E));
        bg.setBorder(BorderFactory.createEmptyBorder(40, 60, 40, 60));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = GridBagConstraints.RELATIVE;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 0, 8, 0);

        JLabel title = new JLabel("TETRIS", SwingConstants.CENTER);
        title.setFont(new Font("Malgun Gothic", Font.BOLD, 36));
        title.setForeground(new Color(0x7DD87A));
        bg.add(title, gbc);

        JLabel sub = new JLabel("모드를 선택하세요", SwingConstants.CENTER);
        sub.setFont(new Font("Malgun Gothic", Font.PLAIN, 14));
        sub.setForeground(new Color(0xA0A0C0));
        bg.add(sub, gbc);

        bg.add(Box.createVerticalStrut(10), gbc);

        JButton btnSingle = createButton("1인 플레이", new Color(0x4A4A8A));
        JButton btnHost   = createButton("2인 대전 - Host (서버 시작)", new Color(0x8A4A4A));
        JButton btnJoin   = createButton("2인 대전 - Join (접속)", new Color(0x4A8A4A));

        btnSingle.addActionListener(e -> {
            dialog.dispose();
            openSinglePlayer();
        });

        btnHost.addActionListener(e -> {
            dialog.dispose();
            startServerAndConnect();
        });

        btnJoin.addActionListener(e -> {
            dialog.dispose();
            joinGame();
        });

        bg.add(btnSingle, gbc);
        bg.add(btnHost, gbc);
        bg.add(btnJoin, gbc);

        dialog.add(bg);
        dialog.pack();
        dialog.setLocationRelativeTo(null);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setVisible(true);
    }

    // ── 1인 플레이 ──────────────────────────────────
    private static void openSinglePlayer() {
        JFrame frame = new JFrame("Tetris - Single Player");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setResizable(false);
        frame.setLayout(new BorderLayout());

        TetrisPanel panel = new TetrisPanel();
        JPanel btnPanel = new JPanel(new GridLayout(1, 2, 8, 0));
        btnPanel.setBackground(new Color(0x16213E));
        btnPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JButton btnPause   = createButton("Pause  [P]",   new Color(0x4A4A8A));
        JButton btnRestart = createButton("Restart  [R]", new Color(0x8A4A4A));
        JButton btnBack    = createButton("Menu",          new Color(0x3A3A3A));

        btnPause.addActionListener(e -> {
            panel.getBoard().togglePause();
            btnPause.setText(panel.getBoard().isPaused() ? "Resume  [P]" : "Pause  [P]");
            if (!panel.getBoard().isPaused()) panel.startTimer();
            panel.requestFocusInWindow();
            panel.repaint();
        });
        btnRestart.addActionListener(e -> {
            panel.getBoard().restart();
            panel.startTimer();
            btnPause.setText("Pause  [P]");
            panel.requestFocusInWindow();
            panel.repaint();
        });
        btnBack.addActionListener(e -> { frame.dispose(); showModeDialog(); });

        panel.setPauseButton(btnPause);
        btnPanel.add(btnPause);
        btnPanel.add(btnRestart);
        btnPanel.add(btnBack);

        frame.add(panel, BorderLayout.CENTER);
        frame.add(btnPanel, BorderLayout.SOUTH);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        frame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowClosed(java.awt.event.WindowEvent e) {
                showModeDialog();
            }
        });
        panel.requestFocusInWindow();
    }

    // ── Host: 서버 실행 후 자동 접속 ────────────────
    private static void startServerAndConnect() {
        // 서버를 백그라운드 스레드로 실행
        new Thread(() -> {
            try {
                java.net.ServerSocket serverSocket = new java.net.ServerSocket(GameServer.PORT);
                serverSocket.setReuseAddress(true);
                System.out.println("[서버] 포트 " + GameServer.PORT + " 대기 중...");
                while (true) {
                    try {
                        System.out.println("[서버] 플레이어 1 대기...");
                        java.net.Socket p1 = serverSocket.accept();
                        System.out.println("[서버] P1 접속: " + p1.getInetAddress().getHostAddress());
                        System.out.println("[서버] 플레이어 2 대기...");
                        java.net.Socket p2 = serverSocket.accept();
                        System.out.println("[서버] P2 접속 - 게임 시작!");
                        // 양쪽 클라이언트 읽기 스레드가 준비될 때까지 대기
                        Thread.sleep(300);
                        sendTo(p1, "START:1");
                        Thread.sleep(50);
                        sendTo(p2, "START:2");
                        System.out.println("[서버] START 전송 완료");
                        new Thread(() -> relayLoop(p1, p2)).start();
                        new Thread(() -> relayLoop(p2, p1)).start();
                    } catch (Exception e) {
                        // 개별 게임 오류는 무시하고 계속 대기
                        System.out.println("[서버] 게임 오류 (재대기): " + e.getMessage());
                    }
                }
            } catch (Exception e) {
                System.out.println("[서버] 치명적 오류: " + e.getMessage());
            }
        }).start();

        // 본인 IP 안내
        String myIp = getLocalIp();
        JOptionPane.showMessageDialog(null,
                "서버 시작됨!\n\n상대방에게 이 IP를 알려주세요:\n" + myIp + "\n\n확인을 누르면 접속합니다.",
                "Host 서버 시작", JOptionPane.INFORMATION_MESSAGE);

        connectToServer("localhost");
    }

    // ── Join: IP 입력 후 접속 ────────────────────────
    private static void joinGame() {
        String ip = JOptionPane.showInputDialog(null,
                "Host의 IP 주소를 입력하세요:", "Join Game", JOptionPane.PLAIN_MESSAGE);
        if (ip == null || ip.trim().isEmpty()) { showModeDialog(); return; }
        connectToServer(ip.trim());
    }

    // ── 서버에 접속하고 멀티플레이어 창 열기 ──────────
    private static void connectToServer(String host) {
        // 1. 게임 창과 패널을 먼저 생성 (리스너 등록 선행)
        NetworkManager network = new NetworkManager();
        openMultiplayerWindow(host, network);
    }

    private static void openMultiplayerWindow(String host, NetworkManager network) {
        JFrame frame = new JFrame("Tetris - 2P Online Battle");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setResizable(false);

        final boolean[] reconnecting = {false};

        frame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowClosing(java.awt.event.WindowEvent e) {
                network.disconnect();
            }
            @Override public void windowClosed(java.awt.event.WindowEvent e) {
                if (!reconnecting[0]) showModeDialog();
            }
        });

        // 2. MultiplayerPanel 먼저 생성 → 리스너 등록됨
        MultiplayerPanel mp = new MultiplayerPanel(network);

        mp.setOnReconnect(() -> {
            reconnecting[0] = true;
            network.disconnect();
            frame.dispose();
            new Thread(() -> {
                try { Thread.sleep(600); } catch (InterruptedException ignored) {}
                SwingUtilities.invokeLater(() -> connectToServer(host));
            }).start();
        });

        JPanel bottom = new JPanel(new GridLayout(1, 2, 8, 0));
        bottom.setBackground(new Color(0x16213E));
        bottom.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JButton btnMenu = createButton("Menu", new Color(0x3A3A3A));
        btnMenu.addActionListener(e -> { network.disconnect(); frame.dispose(); });
        bottom.add(new JLabel());
        bottom.add(btnMenu);

        frame.add(mp, BorderLayout.CENTER);
        frame.add(bottom, BorderLayout.SOUTH);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        mp.requestFocusInWindow();

        // 3. 리스너 등록 완료 후 서버 연결 (START 메시지 놓치지 않도록)
        new Thread(() -> {
            boolean ok = false;
            for (int i = 0; i < 3; i++) {
                ok = network.connect(host, GameServer.PORT);
                if (ok) break;
                try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
                System.out.println("[PC] 서버 재시도 " + (i+1));
            }
            if (!ok) {
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(null,
                            "서버 접속 실패!\nIP: " + host,
                            "접속 오류", JOptionPane.ERROR_MESSAGE);
                    frame.dispose();
                });
            } else {
                System.out.println("[PC] 서버 연결 성공! START 메시지 대기 중...");
            }
        }).start();
    }

    // ── 유틸 ────────────────────────────────────────
    private static void relayLoop(java.net.Socket from, java.net.Socket to) {
        try (java.io.BufferedReader in = new java.io.BufferedReader(
                new java.io.InputStreamReader(from.getInputStream()))) {
            String line;
            while ((line = in.readLine()) != null) sendTo(to, line);
        } catch (Exception e) {
            sendTo(to, "DISCONNECT");
        }
    }

    private static void sendTo(java.net.Socket socket, String msg) {
        try {
            // BufferedWriter 중간 버퍼 없이 직접 전송 (데이터 유실 방지)
            byte[] data = (msg + "\n").getBytes("UTF-8");
            socket.getOutputStream().write(data);
            socket.getOutputStream().flush();
        } catch (Exception ignored) {}
    }

    private static String getLocalIp() {
        try { return java.net.InetAddress.getLocalHost().getHostAddress(); }
        catch (Exception e) { return "127.0.0.1"; }
    }

    static JButton createButton(String text, Color bgColor) {
        JButton btn = new JButton(text);
        btn.setBackground(bgColor);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setFont(new Font("Malgun Gothic", Font.BOLD, 13));
        btn.setPreferredSize(new Dimension(280, 50));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }
}
