package com.portfolio.tetris;

import java.io.*;
import java.net.*;

/**
 * 테트리스 2인 대전 중계 서버
 * 실행: java -cp tetris-pc.jar com.portfolio.tetris.GameServer
 * 포트 9999에서 2명의 플레이어를 연결해 메시지를 중계합니다.
 */
public class GameServer {

    public static final int PORT = 9999;

    public static void main(String[] args) throws Exception {
        System.out.println("=== Tetris Game Server ===");
        System.out.println("Port " + PORT + " 대기 중...");

        ServerSocket serverSocket = new ServerSocket(PORT);

        while (true) {
            System.out.println("\n[대기] 플레이어 1 연결 기다리는 중...");
            Socket p1 = serverSocket.accept();
            System.out.println("[접속] 플레이어 1: " + p1.getInetAddress().getHostAddress());

            System.out.println("[대기] 플레이어 2 연결 기다리는 중...");
            Socket p2 = serverSocket.accept();
            System.out.println("[접속] 플레이어 2: " + p2.getInetAddress().getHostAddress());

            // 두 플레이어 모두 접속 → 게임 시작 신호
            sendMessage(p1, "START:1");
            sendMessage(p2, "START:2");
            System.out.println("[시작] 게임 시작!");

            // 서로 메시지 중계하는 스레드 시작
            Thread t1 = new Thread(() -> relay(p1, p2, "P1"));
            Thread t2 = new Thread(() -> relay(p2, p1, "P2"));
            t1.setDaemon(true);
            t2.setDaemon(true);
            t1.start();
            t2.start();
        }
    }

    private static void relay(Socket from, Socket to, String name) {
        try (BufferedReader in = new BufferedReader(
                new InputStreamReader(from.getInputStream()))) {
            String line;
            while ((line = in.readLine()) != null) {
                sendMessage(to, line);
            }
        } catch (IOException e) {
            System.out.println("[종료] " + name + " 연결 끊김");
            sendMessage(to, "DISCONNECT");
        }
    }

    private static void sendMessage(Socket socket, String message) {
        try {
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            out.println(message);
        } catch (IOException ignored) {}
    }
}
