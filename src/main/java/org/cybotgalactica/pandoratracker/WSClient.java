package org.cybotgalactica.pandoratracker;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.io.Closeable;
import java.net.URI;
import java.net.URISyntaxException;

public class WSClient implements Closeable {

    private final int restartTimeout = 5000;
    private WebSocketClient webSocketClient;
    private boolean closed = false;
    private Runnable restartTask;

    WSClient(PandoraTracker tracker, String address, MessageHandler messageHandler) {
        restartTask = () -> {
            try {
                Thread.sleep(restartTimeout);
                restart(tracker, address, messageHandler);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        };
        restart(tracker, address, messageHandler);
    }

    private void restart(PandoraTracker tracker, String address, MessageHandler messageHandler) {
        closed = false;
        try {
            webSocketClient = new WebSocketClient(new URI(address)) {
                @Override
                public void onOpen(ServerHandshake handshakedata) {
                }

                @Override
                public void onMessage(String message) {
                    messageHandler.onMessage(message);
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    if (!closed) {
                        setRestart();
                    }
                }

                @Override
                public void onError(Exception ex) {
                    tracker.debug("Websocket restarting");
                }
            };
            webSocketClient.connect();
        } catch (URISyntaxException e) {
            e.printStackTrace();
            tracker.debug("Error during restarting WebSockets! PANIQUE!!!");
        }
    }

    private void setRestart() {
        Thread restartThread = new Thread(restartTask);
        restartThread.setDaemon(true);
        restartThread.start();
    }

    public void close() {
        closed = true;
        try {
            webSocketClient.closeBlocking();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @FunctionalInterface
    interface MessageHandler {
        void onMessage(String message);
    }
}

