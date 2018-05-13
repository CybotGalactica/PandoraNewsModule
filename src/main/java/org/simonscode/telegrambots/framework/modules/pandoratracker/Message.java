package org.simonscode.telegrambots.framework.modules.pandoratracker;

import org.telegram.telegrambots.api.methods.send.SendMessage;

public class Message {

    private final Type type;
    private final String input;

    public Message(Type type, String input) {
        this.type = type;
        this.input = input;
    }

    public SendMessage getSendMessage() {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setText(String.format("[%s] %s", type.getValue(), input));
        return sendMessage;
    }

    enum Type {
        NEWS("News"),
        KILLFEED("Kill"),
        KILLSHOUT("Kill"),
        PUZZLE("Puzzle solved");

        private final String value;

        Type(String value) {
            this.value = value;
        }

        String getValue() {
            return value;
        }
    }
}
