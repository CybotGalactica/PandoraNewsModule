package org.simonscode.telegrambots.framework.modules.pandoratracker;

class Update {

    private final String message;

    public Update(String message) {
        this.message = message;
    }

    public String getText() {
        return String.format("%s", message);
    }
}
