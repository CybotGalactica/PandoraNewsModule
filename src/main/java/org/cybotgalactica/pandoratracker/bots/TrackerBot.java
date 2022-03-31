package org.cybotgalactica.pandoratracker.bots;

import org.cybotgalactica.pandoratracker.models.BotMessage;

public interface TrackerBot {
    void sendMessage(BotMessage message);
    void sendDebugIfAvailable(BotMessage debugMessage);
    void setTestMode(boolean isTestMode);
}
