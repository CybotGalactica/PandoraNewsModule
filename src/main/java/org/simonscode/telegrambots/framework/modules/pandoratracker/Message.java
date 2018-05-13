package org.simonscode.telegrambots.framework.modules.pandoratracker;

import org.telegram.telegrambots.api.methods.send.SendMessage;

public class Message {

    private final String input;

    public Message(String input) {

        this.input = input;
    }

    public SendMessage getSendMessage(){
        SendMessage sendMessage = new SendMessage();
        sendMessage.setText(input);
        return sendMessage;
    }

    enum Type {
        NEWS, KILLFEED, KILLSHOUT, PUZZLE
    }
}
