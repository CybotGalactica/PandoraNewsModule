package org.simonscode.telegrambots.framework.modules.pandoratracker;

public class Team {
    private final String name;
    private final String alias;
    public final int userId;

    public Team(String name, String alias, int userId) {
        this.name = name;
        this.alias = alias;
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public int getUserId() {
        return userId;
    }

    public String getAlias() {
        return alias;
    }
}
