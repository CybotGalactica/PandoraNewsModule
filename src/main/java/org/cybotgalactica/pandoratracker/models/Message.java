package org.cybotgalactica.pandoratracker.models;

public class Message {
    public String getText() {
        return text;
    }

    private final String text;

    public Message(String text) {
        this.text = text;
    }
}
