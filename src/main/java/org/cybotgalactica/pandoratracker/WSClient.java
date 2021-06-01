package org.cybotgalactica.pandoratracker;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.io.Closeable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Timer;
import java.util.TimerTask;

public class WSClient implements Closeable {

    private final int restartTimeout = 5000;
    private final int restartInterval = 60000;
    private WebSocketClient webSocketClient;
    private boolean closed = false;
    private final TimerTask restartTask;
    private final Timer timer;

    WSClient(PandoraTracker tracker, String address, MessageHandler messageHandler) {
        restartTask = new TimerTask() {
            @Override
            public void run() {
                restart(tracker, address, messageHandler);
            }
        };
        timer = new Timer();
        restart(tracker, address, messageHandler);
        timer.scheduleAtFixedRate(restartTask, restartInterval, restartInterval);
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
                    System.out.println(message);
                    messageHandler.onMessage(message);
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    tracker.debug("Websocket closed");
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
        timer.schedule(restartTask, restartTimeout);
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

