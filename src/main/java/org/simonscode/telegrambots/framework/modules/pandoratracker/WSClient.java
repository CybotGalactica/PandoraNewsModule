package org.simonscode.telegrambots.framework.modules.pandoratracker;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.io.Closeable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Timer;
import java.util.TimerTask;

public class WSClient implements Closeable {

    private static Timer restartTimer;
    private final int restartTimeout = 5000;
    private WebSocketClient webSocketClient;
    private boolean closed = false;
    private TimerTask restartTask;

    public WSClient(PandoraTracker tracker, Message.Type type, String address, MessageHandler messageHandler) {
        restartTimer = new Timer();
        restartTask = new TimerTask() {
            @Override
            public void run() {
                try {
                    restart(tracker, type, address, messageHandler);
                } catch (URISyntaxException e) {
                    restartTimer = new Timer();
                    restartTimer.schedule(restartTask, restartTimeout);
                    e.printStackTrace();
                }
            }
        };

        try {
            restart(tracker, type, address, messageHandler);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    private void restart(PandoraTracker tracker, Message.Type type, String address, MessageHandler messageHandler) throws URISyntaxException {
        webSocketClient = new WebSocketClient(new URI(address)) {
            @Override
            public void onOpen(ServerHandshake handshakedata) {
                tracker.sendDebug("Connected: " + type.getValue());
            }

            @Override
            public void onMessage(String message) {
                messageHandler.onMessage(type, message);
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
                if (!closed) {
                    restartTimer.schedule(restartTask, restartTimeout);
                    tracker.sendDebug("Websocket closed: " + type.getValue());
                }
            }

            @Override
            public void onError(Exception ex) {
                tracker.sendDebug("Websocket Error: " + ex.getMessage());
            }
        };
        webSocketClient.connect();
        restartTimer = new Timer();
    }

    public void close() {
        closed = true;
        restartTimer.cancel();
        try {
            webSocketClient.closeBlocking();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @FunctionalInterface
    interface MessageHandler {
        void onMessage(Message.Type type, String message);
    }
}

