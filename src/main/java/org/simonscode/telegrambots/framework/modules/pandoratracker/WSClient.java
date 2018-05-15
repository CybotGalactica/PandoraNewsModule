package org.simonscode.telegrambots.framework.modules.pandoratracker;

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

    public WSClient(PandoraTracker tracker, Update.Type type, String address, MessageHandler messageHandler) {
        restartTask = () -> {
        try {
            Thread.sleep(restartTimeout);
            restart(tracker, type, address, messageHandler);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    };
        restart(tracker, type, address, messageHandler);
    }

    private void setRestart(){
        Thread restartThread = new Thread(restartTask);
        restartThread.setDaemon(true);
        restartThread.start();
    }

    private void restart(PandoraTracker tracker, Update.Type type, String address, MessageHandler messageHandler) {
        closed = false;
        try {
            webSocketClient = new WebSocketClient(new URI(address)) {
                @Override
                public void onOpen(ServerHandshake handshakedata) {
                    tracker.debug("Connected: " + type.getValue());
                }

                @Override
                public void onMessage(String message) {
                    messageHandler.onMessage(type, message);
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    if (!closed) {
                        setRestart();
                        tracker.debug("Websocket closed: " + type.getValue());
                    }
                }

                @Override
                public void onError(Exception ex) {
                    tracker.debug("Websocket Error: " + ex.getMessage());
                }
            };
            webSocketClient.connect();
        }catch (URISyntaxException e) {

        }
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
        void onMessage(Update.Type type, String message);
    }
}

