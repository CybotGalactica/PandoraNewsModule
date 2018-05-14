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
    private WebSocketClient webSocketClient;
    private boolean closed = false;
    private TimerTask restartTask;
    private final int restartTimeout = 5000;

    public WSClient(PandoraTracker tracker, Message.Type type, String address, MessageHandler messageHandler) {
        restartTimer = new Timer();
        restartTask = new TimerTask() {
            @Override
            public void run() {
                try {
                    restart(tracker, type, address, messageHandler);
                } catch (URISyntaxException e) {
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
                tracker.sendDebug("(Re)Connected: " + address);
            }

            @Override
            public void onMessage(String message) {
                messageHandler.onMessage(type, message);
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
                if (!closed) {
                    restartTimer.schedule(restartTask, restartTimeout);
                    tracker.sendDebug("Warning: Websocket was closed: " + address);
                }
            }

            @Override
            public void onError(Exception ex) {
                tracker.sendDebug("Warning: Websocket experienced an error: " + ex.getMessage());
            }
        };
        webSocketClient.connect();
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

