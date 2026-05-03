package com.portfolio.tetris;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Tetris - PC Version");
            TetrisPanel panel = new TetrisPanel();
            frame.add(panel);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(false);
            frame.pack();
            frame.setLocationRelativeTo(null); // 화면 중앙
            frame.setVisible(true);
            panel.requestFocusInWindow();
        });
    }
}
