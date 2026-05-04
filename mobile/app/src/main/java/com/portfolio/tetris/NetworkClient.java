package com.portfolio.tetris;

import android.util.Log;
import java.io.*;
import java.net.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NetworkClient {

    public static final int PORT = 9999;
    private static final String TAG = "TetrisNetwork";

    private Socket socket;
    private PrintWriter out;
    private MessageListener listener;
    private boolean connected = false;

    // 전송 전용 단일 스레드 (순서 보장 + 메인 스레드 블로킹 방지)
    private final ExecutorService sendExecutor = Executors.newSingleThreadExecutor();

    public interface MessageListener {
        void onMessage(String message);
        void onDisconnected();
    }

    public void setListener(MessageListener listener) { this.listener = listener; }
    public boolean isConnected() { return connected; }

    // 반드시 백그라운드 스레드에서 호출
    public boolean connect(String host, int port) {
        try {
            Log.i(TAG, "서버 연결 시도: " + host + ":" + port);
            socket = new Socket();
            socket.connect(new InetSocketAddress(host, port), 5000);
            out = new PrintWriter(new BufferedWriter(
                    new OutputStreamWriter(socket.getOutputStream())), true);
            connected = true;
            Log.i(TAG, "서버 연결 성공!");
            startReading();
            return true;
        } catch (IOException e) {
            Log.e(TAG, "서버 연결 실패: " + e.getMessage());
            return false;
        }
    }

    // 백그라운드 스레드에서 전송 → NetworkOnMainThreadException 해결
    public void send(String message) {
        if (!connected || out == null) return;
        sendExecutor.submit(() -> {
            try {
                if (!message.startsWith("BOARD:"))
                    Log.d(TAG, "전송: " + message);
                out.println(message);
            } catch (Exception e) {
                Log.e(TAG, "전송 실패: " + e.getMessage());
            }
        });
    }

    public void disconnect() {
        connected = false;
        sendExecutor.shutdown();
        Log.i(TAG, "연결 종료");
        try { if (socket != null) socket.close(); } catch (IOException ignored) {}
    }

    private void startReading() {
        Thread readThread = new Thread(() -> {
            try (BufferedReader in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()))) {
                String line;
                while ((line = in.readLine()) != null) {
                    if (!line.startsWith("BOARD:"))
                        Log.d(TAG, "수신: " + line);
                    MessageListener l = listener; // 로컬 참조 (null 안전)
                    if (l != null) l.onMessage(line);
                }
            } catch (IOException e) {
                Log.e(TAG, "연결 끊김: " + e.getMessage());
                connected = false;
            } finally {
                MessageListener l = listener;
                if (l != null) l.onDisconnected();
            }
        });
        readThread.setDaemon(true);
        readThread.start();
    }
}
