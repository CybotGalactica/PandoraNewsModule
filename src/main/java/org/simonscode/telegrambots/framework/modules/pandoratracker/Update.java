package org.simonscode.telegrambots.framework.modules.pandoratracker;

class Update {

    private final String text;
    private Type type;
    private String subject;
    private String object;

    Update(Type type, String input) {
        text = input;
        this.type = type;
        try {
            switch (type) {
                case KILLFEED:
                case KILLSHOUT:
                    String[] split = input.substring(0, input.length() - 8).split(" pwned ");
                    subject = split[0];
                    object = split[1];
                    break;
            }
        } catch (Throwable t) {
            t.printStackTrace();
            this.type = Type.ERROR;
        }
    }

    String getText() {
        switch (type) {
            case KILLSHOUT:
            case KILLFEED:
                return subject + " killed " + object;
            default:
                return text;
        }
    }

    enum Type {
        NEWS, KILLFEED, KILLSHOUT, PUZZLE, ERROR
    }
}
