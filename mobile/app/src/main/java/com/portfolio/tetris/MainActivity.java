package com.portfolio.tetris;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private TetrisView tetrisView;
    private Button btnPause, btnRestart, btnMultiplayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);

        tetrisView      = findViewById(R.id.tetrisView);
        btnPause        = findViewById(R.id.btnPause);
        btnRestart      = findViewById(R.id.btnRestart);
        btnMultiplayer  = findViewById(R.id.btnMultiplayer);

        btnPause.setOnClickListener(v -> {
            tetrisView.togglePause();
            btnPause.setText(tetrisView.getBoard().isPaused() ? "재개" : "일시정지");
        });

        btnRestart.setOnClickListener(v -> {
            tetrisView.restart();
            btnPause.setText("일시정지");
        });

        btnMultiplayer.setOnClickListener(v -> showMultiplayerDialog());

        tetrisView.requestFocus();
    }

    // 2인 대전 - PC가 항상 서버, 모바일은 접속만
    private void showMultiplayerDialog() {
        tetrisView.pauseGame();

        EditText input = new EditText(this);
        input.setHint("PC 서버 IP 주소 입력 (예: 192.168.0.x)");
        input.setText(getLastIp());

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 24, 48, 0);

        android.widget.TextView guide = new android.widget.TextView(this);
        guide.setText("① PC에서 게임 실행 → '2인 대전 - Host' 클릭\n② PC의 IP 주소 확인 후 아래에 입력\n③ 접속 버튼 클릭");
        guide.setTextSize(13f);
        guide.setPadding(0, 0, 0, 16);
        layout.addView(guide);
        layout.addView(input);

        new AlertDialog.Builder(this)
                .setTitle("2인 대전 접속")
                .setView(layout)
                .setPositiveButton("접속", (d, w) -> {
                    String ip = input.getText().toString().trim();
                    if (!ip.isEmpty()) {
                        saveLastIp(ip);
                        startMultiplayer(ip);
                    }
                })
                .setNegativeButton("취소", (d, w) -> tetrisView.resumeLoop())
                .show();
    }

    // 마지막 접속 IP 저장/불러오기
    private void saveLastIp(String ip) {
        getSharedPreferences("tetris", MODE_PRIVATE).edit().putString("last_ip", ip).apply();
    }
    private String getLastIp() {
        return getSharedPreferences("tetris", MODE_PRIVATE).getString("last_ip", "");
    }

    private void startMultiplayer(String host) {
        Intent intent = new Intent(this, MultiplayerActivity.class);
        intent.putExtra("host", host);
        startActivity(intent);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (tetrisView.getBoard().isGameOver()) {
            if (keyCode == KeyEvent.KEYCODE_SPACE || keyCode == KeyEvent.KEYCODE_ENTER) {
                tetrisView.restart();
                btnPause.setText("일시정지");
                return true;
            }
        }
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_LEFT:
            case KeyEvent.KEYCODE_A:   tetrisView.handleLeft();     return true;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
            case KeyEvent.KEYCODE_D:   tetrisView.handleRight();    return true;
            case KeyEvent.KEYCODE_DPAD_UP:
            case KeyEvent.KEYCODE_W:   tetrisView.handleRotate();   return true;
            case KeyEvent.KEYCODE_DPAD_DOWN:
            case KeyEvent.KEYCODE_S:   tetrisView.handleDown();     return true;
            case KeyEvent.KEYCODE_SPACE: tetrisView.handleHardDrop(); return true;
            case KeyEvent.KEYCODE_P:
                tetrisView.togglePause();
                btnPause.setText(tetrisView.getBoard().isPaused() ? "재개" : "일시정지");
                return true;
            case KeyEvent.KEYCODE_R:
                tetrisView.restart();
                btnPause.setText("일시정지");
                return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onPause() {
        super.onPause();
        tetrisView.pauseGame();
        btnPause.setText("재개");
    }

    @Override
    protected void onResume() {
        super.onResume();
        tetrisView.requestFocus();
    }
}
