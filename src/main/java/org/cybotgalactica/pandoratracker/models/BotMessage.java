package org.cybotgalactica.pandoratracker.models;

import java.util.List;

public class BotMessage {
    final CollectionType collectionType;
    final Type type;
    final Object message;

    public BotMessage(String message) {
        this.message = message;
        type = Type.PLAIN;
        collectionType = CollectionType.SINGLE;
    }

    public BotMessage(List<String> message) {
        this.message = message;
        type = Type.PLAIN;
        collectionType = CollectionType.LIST;
    }

}

enum CollectionType {
    SINGLE, LIST
}

enum Type {
    PLAIN
}