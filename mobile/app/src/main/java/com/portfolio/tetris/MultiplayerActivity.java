package com.portfolio.tetris;

import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MultiplayerActivity extends AppCompatActivity {

    private TetrisView myView;
    private OpponentView opponentView;
    private TextView tvStatus, tvMyScore, tvOpScore;
    private Button btnLeft, btnRight, btnRotate, btnHold, btnDrop;

    private NetworkClient network;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private String serverHost;
    private int myNumber = 0;
    private boolean gameStarted = false;
    private boolean resultShown = false;

    // 네트워크 전송 타이머
    private final Runnable networkTask = new Runnable() {
        @Override
        public void run() {
            if (gameStarted && network != null && network.isConnected()) {
                TetrisBoard board = myView.getBoard();
                network.send("BOARD:" + board.serializeBoard());
                network.send("SCORE:" + board.getScore() + "," + board.getLevel() + "," + board.getLinesCleared());
                int atk = board.getAndResetAttack();
                if (atk > 0) network.send("ATTACK:" + atk);
                if (board.isGameOver()) {
                    network.send("GAMEOVER");
                    handler.removeCallbacks(this);
                    if (!isFinishing() && !isDestroyed())
                        showResult("게임 오버...", false);
                    return;
                }
            }
            handler.postDelayed(this, 200);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_multiplayer);

        myView       = findViewById(R.id.myTetrisView);
        opponentView = findViewById(R.id.opponentView);
        tvStatus     = findViewById(R.id.tvStatus);
        tvMyScore    = findViewById(R.id.tvMyScore);
        tvOpScore    = findViewById(R.id.tvOpScore);
        btnLeft      = findViewById(R.id.btnLeft);
        btnRight     = findViewById(R.id.btnRight);
        btnRotate    = findViewById(R.id.btnRotate);
        btnHold      = findViewById(R.id.btnHold);
        btnDrop      = findViewById(R.id.btnDrop);

        // 버튼 클릭 리스너
        btnLeft.setOnClickListener(v   -> { myView.getBoard().moveLeft();  myView.invalidate(); });
        btnRight.setOnClickListener(v  -> { myView.getBoard().moveRight(); myView.invalidate(); });
        btnRotate.setOnClickListener(v -> { myView.getBoard().rotate();    myView.invalidate(); });
        btnHold.setOnClickListener(v   -> { myView.getBoard().holdBlock(); myView.invalidate(); });
        btnDrop.setOnClickListener(v   -> { myView.getBoard().hardDrop();  myView.invalidate(); });

        serverHost = getIntent().getStringExtra("host");
        connectToServer();
    }

    private void connectToServer() {
        // 기존 게임 완전 정리
        handler.removeCallbacks(networkTask);
        myView.pauseGame();
        if (network != null) {
            network.setListener(null); // 콜백 먼저 제거 (onDisconnected 방지)
            network.disconnect();
        }

        tvStatus.setText("서버 연결 중...");
        gameStarted = false;
        resultShown = false;
        myNumber = 0;

        network = new NetworkClient();

        network.setListener(new NetworkClient.MessageListener() {
            @Override
            public void onMessage(String msg) {
                handler.post(() -> handleMessage(msg));
            }
            @Override
            public void onDisconnected() {
                handler.post(() -> {
                    if (!resultShown && !isFinishing() && !isDestroyed()) {
                        tvStatus.setText("연결 끊김");
                        showReconnectDialog("서버와 연결이 끊겼습니다.");
                    }
                });
            }
        });

        new Thread(() -> {
            // 서버 정리 시간 확보 후 연결 (최대 3회 재시도)
            boolean ok = false;
            for (int i = 0; i < 3; i++) {
                try { Thread.sleep(i == 0 ? 500 : 1500); } catch (InterruptedException ignored) {}
                ok = network.connect(serverHost, NetworkClient.PORT);
                if (ok) break;
                System.out.println("[네트워크] 접속 실패, 재시도 " + (i + 1));
            }
            final boolean connected = ok;
            handler.post(() -> {
                if (connected) {
                    tvStatus.setText("상대방 기다리는 중...");
                } else {
                    Toast.makeText(this, "서버 접속 실패! 3회 시도 후 실패\nIP: " + serverHost, Toast.LENGTH_LONG).show();
                    finish();
                }
            });
        }).start();
    }

    private void handleMessage(String msg) {
        if (msg.startsWith("START:")) {
            myNumber = Integer.parseInt(msg.substring(6));
            tvStatus.setText("P" + myNumber + " 게임 시작!");
            gameStarted = true;
            myView.getBoard().restart();
            opponentView.setGameOver(false);
            myView.startGameLoop();
            handler.post(networkTask);

        } else if (msg.startsWith("BOARD:")) {
            opponentView.updateBoard(msg.substring(6));

        } else if (msg.startsWith("SCORE:")) {
            String[] parts = msg.substring(6).split(",");
            tvOpScore.setText("OP: " + parts[0]);
            tvMyScore.setText("ME: " + myView.getBoard().getScore());

        } else if (msg.startsWith("ATTACK:")) {
            int lines = Integer.parseInt(msg.substring(7));
            myView.getBoard().addGarbageLines(lines);
            tvStatus.setText("공격 +" + lines + "줄!");
            handler.postDelayed(() -> tvStatus.setText("P" + myNumber), 1500);

        } else if (msg.equals("GAMEOVER")) {
            opponentView.setGameOver(true);
            handler.removeCallbacks(networkTask);
            if (!isFinishing() && !isDestroyed())
                showResult("승리!", true);

        } else if (msg.equals("DISCONNECT")) {
            handler.removeCallbacks(networkTask);
            if (!isFinishing() && !isDestroyed())
                showReconnectDialog("상대방이 연결을 끊었습니다.");
        }

        tvMyScore.setText("ME: " + myView.getBoard().getScore());
    }

    // 게임 결과 다이얼로그 - 재접속 or 나가기
    private void showResult(String message, boolean win) {
        if (resultShown || isFinishing() || isDestroyed()) return;
        resultShown = true;
        myView.pauseGame();
        handler.removeCallbacks(networkTask);

        new AlertDialog.Builder(this)
                .setTitle(win ? "승리!" : "게임 오버")
                .setMessage(message + "\n내 점수: " + myView.getBoard().getScore()
                        + "\n\n다시 대전하려면 재접속하세요.")
                .setPositiveButton("재접속", (d, w) -> {
                    tvStatus.setText("재접속 중...");
                    handler.postDelayed(() -> connectToServer(), 600);
                })
                .setNegativeButton("나가기", (d, w) -> finish())
                .setCancelable(false)
                .show();
    }

    // 연결 끊김 재연결 다이얼로그
    private void showReconnectDialog(String reason) {
        if (resultShown || isFinishing() || isDestroyed()) return;
        resultShown = true;
        handler.removeCallbacks(networkTask);

        new AlertDialog.Builder(this)
                .setTitle("연결 끊김")
                .setMessage(reason + "\n재접속하시겠습니까?")
                .setPositiveButton("재접속", (d, w) -> {
                    tvStatus.setText("재접속 중...");
                    handler.postDelayed(() -> connectToServer(), 600);
                })
                .setNegativeButton("나가기", (d, w) -> finish())
                .setCancelable(false)
                .show();
    }

    // 뒤로가기 버튼 → 확인 다이얼로그
    @Override
    public void onBackPressed() {
        if (isFinishing() || isDestroyed()) return;
        new AlertDialog.Builder(this)
                .setTitle("나가기")
                .setMessage("게임을 종료하고 나가시겠습니까?")
                .setPositiveButton("나가기", (d, w) -> {
                    handler.removeCallbacks(networkTask);
                    myView.pauseGame();
                    if (network != null) network.disconnect();
                    finish();
                })
                .setNegativeButton("계속하기", null)
                .show();
    }

    @Override
    protected void onPause() {
        super.onPause();
        myView.pauseGame();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(networkTask);
        myView.pauseGame();
        if (network != null) network.disconnect();
    }
}
