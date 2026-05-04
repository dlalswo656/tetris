package com.portfolio.tetris;

import java.io.*;
import java.net.*;
import java.util.function.Consumer;

public class NetworkManager {

    private Socket socket;
    private PrintWriter out;
    private Thread readThread;
    private Consumer<String> onMessage;
    private Runnable onDisconnect;
    private boolean connected = false;

    public void setOnMessage(Consumer<String> onMessage) { this.onMessage = onMessage; }
    public void setOnDisconnect(Runnable onDisconnect)   { this.onDisconnect = onDisconnect; }
    public boolean isConnected() { return connected; }

    public boolean connect(String host, int port) {
        try {
            socket = new Socket(host, port);
            out = new PrintWriter(socket.getOutputStream(), true);
            connected = true;
            startReading();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public void send(String message) {
        if (connected && out != null) out.println(message);
    }

    public void disconnect() {
        connected = false;
        try { if (socket != null) socket.close(); } catch (IOException ignored) {}
    }

    private void startReading() {
        readThread = new Thread(() -> {
            try (BufferedReader in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()))) {
                String line;
                while ((line = in.readLine()) != null) {
                    final String msg = line;
                    if (onMessage != null) onMessage.accept(msg);
                }
            } catch (IOException e) {
                connected = false;
            } finally {
                if (onDisconnect != null) onDisconnect.run();
            }
        });
        readThread.setDaemon(true);
        readThread.start();
    }
}
