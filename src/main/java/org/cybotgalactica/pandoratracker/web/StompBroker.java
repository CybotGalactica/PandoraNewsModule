package org.cybotgalactica.pandoratracker.web;

import org.cybotgalactica.pandoratracker.controller.PandoraTracker;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

@Controller
public class StompBroker {
    private final PandoraTracker pandoraTracker;

    public StompBroker(PandoraTracker pandoraTracker) {
        this.pandoraTracker = pandoraTracker;
    }

    @MessageMapping("/")
    public void handleMessage(String message) {
        pandoraTracker.onUpdate(message);
    }
}
