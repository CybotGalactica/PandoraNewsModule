package org.cybotgalactica.pandoratracker.controller;

import org.cybotgalactica.pandoratracker.bots.TrackerBot;
import org.cybotgalactica.pandoratracker.models.BotMessage;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class BotRegistry {
    private final AtomicInteger idCounter = new AtomicInteger();
    private final ConcurrentHashMap<Integer, TrackerBot> bots = new ConcurrentHashMap<>();
    private final PandoraTracker pandoraTracker;

    public BotRegistry(PandoraTracker pandoraTracker) {
        this.pandoraTracker = pandoraTracker;
    }

    public BotControlInterface registerBot(TrackerBot bot) {
        Integer id = idCounter.incrementAndGet();
        bots.put(id, bot);
        sendDebug(new BotMessage(String.format("Registered bot of class %s with id %d", bot.getClass().toString(), id)));
        return new BotControlInterface(id, pandoraTracker, this);
    }

    public void deregisterBot(Integer id) {
        bots.remove(id);
    }

    public void sendMessage(BotMessage message) {
        bots.values().forEach(b -> b.sendMessage(message));
    }

    public void sendDebug(BotMessage debugMessage) {
        bots.values().forEach(b -> b.sendDebugIfAvailable(debugMessage));
    }
}
