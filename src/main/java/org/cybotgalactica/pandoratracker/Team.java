package org.cybotgalactica.pandoratracker;

@SuppressWarnings("unused")
public class Team {
    private final String name;
    private final String alias;
    private final int userId;

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
