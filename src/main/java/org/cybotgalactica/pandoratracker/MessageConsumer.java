package org.cybotgalactica.pandoratracker;

import org.cybotgalactica.pandoratracker.models.Message;

public interface MessageConsumer {
    void consumeMessage(Message message);
}
