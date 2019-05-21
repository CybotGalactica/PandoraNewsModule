package org.simonscode.telegrambots.framework.modules.pandoratracker;

import org.telegram.telegrambots.api.methods.send.SendMessage;

public class Update {

    private final String message;

    public Update(String message) {
        this.message = message;
    }

    public SendMessage getSendMessage() {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setText(getText());
        return sendMessage;
    }

    public String getText() {
        return String.format("%s", message);
    }
}
