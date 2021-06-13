package org.cybotgalactica.pandoratracker;

public class Update {

    private final Type type;
    private final String message;

    public Update(Type type, String message) {
        this.type = type;
        this.message = message;
    }

    public String getText() {
        return String.format("%s   %s", type == null ? "❓" : type.getIcon(), message);
    }

    enum Type {
        news("\uD83D\uDCE3"),
        kill("\uD83D\uDD2B"),
        puzzle("\uD83E\uDDE9");

        private final String icon;

        Type(String icon) {
            this.icon = icon;
        }

        String getIcon() {
            return icon;
        }
    }
}
