package com.portfolio.tetris;

import javax.swing.*;
import java.awt.*;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Tetris - PC Version");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(false);
            frame.setLayout(new BorderLayout());

            TetrisPanel panel = new TetrisPanel();

            // 하단 버튼 패널
            JPanel btnPanel = new JPanel();
            btnPanel.setBackground(new Color(0x16213E));
            btnPanel.setLayout(new GridLayout(1, 2, 8, 0));
            btnPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

            JButton btnPause   = createButton("Pause  [P]",   new Color(0x4A4A8A));
            JButton btnRestart = createButton("Restart  [R]", new Color(0x8A4A4A));

            btnPause.addActionListener(e -> {
                panel.getBoard().togglePause();
                if (panel.getBoard().isPaused()) {
                    btnPause.setText("Resume  [P]");
                } else {
                    btnPause.setText("Pause  [P]");
                    panel.startTimer();
                }
                panel.requestFocusInWindow();
                panel.repaint();
            });

            btnRestart.addActionListener(e -> {
                panel.getBoard().restart();
                panel.startTimer();
                btnPause.setText("⏸  Pause");
                panel.requestFocusInWindow();
                panel.repaint();
            });

            // P키와 버튼 상태 동기화
            panel.setPauseButton(btnPause);

            btnPanel.add(btnPause);
            btnPanel.add(btnRestart);

            frame.add(panel, BorderLayout.CENTER);
            frame.add(btnPanel, BorderLayout.SOUTH);

            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
            panel.requestFocusInWindow();
        });
    }

    private static JButton createButton(String text, Color bgColor) {
        JButton btn = new JButton(text);
        btn.setBackground(bgColor);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setFont(new Font("Arial", Font.BOLD, 14));
        btn.setPreferredSize(new Dimension(0, 44));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }
}
