package org.cybotgalactica.pandoratracker.controller;

import lombok.Getter;

public class BotControlInterface {
    @Getter
    private Integer id;
    private final PandoraTracker pandoraTracker;
    private BotRegistry registry;

    public BotControlInterface(Integer id, PandoraTracker pandoraTracker, BotRegistry registry) {
        this.id = id;
        this.pandoraTracker = pandoraTracker;
        this.registry = registry;
    }

    public boolean isPaused() {
        return pandoraTracker.getIsPaused().get();
    }

    public void setPaused(boolean paused) {
        pandoraTracker.getIsPaused().set(paused);
    }

    public void deregister() {
        registry.deregisterBot(id);
    }

    public boolean isTestMode() {
        return pandoraTracker.
    }
}
