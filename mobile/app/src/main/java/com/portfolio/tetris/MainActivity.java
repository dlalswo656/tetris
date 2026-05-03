package com.portfolio.tetris;

import android.os.Bundle;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private TetrisView tetrisView;
    private Button btnPause, btnRestart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);

        tetrisView = findViewById(R.id.tetrisView);
        btnPause   = findViewById(R.id.btnPause);
        btnRestart = findViewById(R.id.btnRestart);

        btnPause.setOnClickListener(v -> {
            tetrisView.togglePause();
            btnPause.setText(tetrisView.getBoard().isPaused() ? "▶ 재개" : "⏸ 일시정지");
        });

        btnRestart.setOnClickListener(v -> {
            tetrisView.restart();
            btnPause.setText("⏸ 일시정지");
        });

        tetrisView.requestFocus();
    }

    // ── 키보드 조작 ──────────────────────────────
    // ← → : 좌우 이동
    // ↑    : 회전
    // ↓    : 소프트드롭
    // Space: 하드드롭 (즉시 낙하)
    // P    : 일시정지
    // R    : 다시시작
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (tetrisView.getBoard().isGameOver()) {
            if (keyCode == KeyEvent.KEYCODE_SPACE || keyCode == KeyEvent.KEYCODE_ENTER) {
                tetrisView.restart();
                btnPause.setText("⏸ 일시정지");
                return true;
            }
        }
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_LEFT:
            case KeyEvent.KEYCODE_A:
                tetrisView.handleLeft();
                return true;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
            case KeyEvent.KEYCODE_D:
                tetrisView.handleRight();
                return true;
            case KeyEvent.KEYCODE_DPAD_UP:
            case KeyEvent.KEYCODE_W:
                tetrisView.handleRotate();
                return true;
            case KeyEvent.KEYCODE_DPAD_DOWN:
            case KeyEvent.KEYCODE_S:
                tetrisView.handleDown();
                return true;
            case KeyEvent.KEYCODE_SPACE:
                tetrisView.handleHardDrop();
                return true;
            case KeyEvent.KEYCODE_P:
                tetrisView.togglePause();
                btnPause.setText(tetrisView.getBoard().isPaused() ? "▶ 재개" : "⏸ 일시정지");
                return true;
            case KeyEvent.KEYCODE_R:
                tetrisView.restart();
                btnPause.setText("⏸ 일시정지");
                return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // 앱이 백그라운드로 갈 때만 pause (이미 paused면 무시)
        tetrisView.pauseGame();
        btnPause.setText("▶ 재개");
    }

    @Override
    protected void onResume() {
        super.onResume();
        tetrisView.requestFocus();
    }
}
