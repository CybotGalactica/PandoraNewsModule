package org.cybotgalactica.pandoratracker;

import jdk.nashorn.internal.objects.annotations.Setter;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

@Controller
public class StompBroker {
    private MessageHandler messageHandler;

    @MessageMapping("/")
    public void handleMessage(String message) {
        this.messageHandler.onMessage(message);
    }

    public void setMessageHandler(MessageHandler messageHandler) {
        this.messageHandler = messageHandler;
    }

    @FunctionalInterface
    interface MessageHandler {
        void onMessage(String message);
    }
}
