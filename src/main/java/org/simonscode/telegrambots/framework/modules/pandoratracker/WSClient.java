package org.simonscode.telegrambots.framework.modules.pandoratracker;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.io.Closeable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Timer;
import java.util.TimerTask;

public class WSClient implements Closeable {

    private static Timer restartTimer = new Timer();
    private WebSocketClient webSocketClient;
    private boolean closed = false;

    public WSClient(PandoraTracker tracker, Message.Type type, String address, MessageHandler messageHandler) {
        try {
            webSocketClient = new WebSocketClient(new URI(address)) {
                @Override
                public void onOpen(ServerHandshake handshakedata) {
                    System.out.println("Connected: " + address);
                }

                @Override
                public void onMessage(String message) {
                    messageHandler.onMessage(type, message);
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    if (!closed) {
                        restartTimer.schedule(new TimerTask() {
                            @Override
                            public void run() {
                                webSocketClient.connect();
                            }
                        }, 5_000);
                        tracker.sendDebug("Warning: Websocket was closed: " + address);
                    }
                }

                @Override
                public void onError(Exception ex) {
                    tracker.sendDebug("Warning: Websocket experienced an error: " + ex.getMessage());
                }
            };
            webSocketClient.connect();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
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

