package org.simonscode.telegrambots.framework.modules.pandoratracker;

public class Team {
    private final String name;
    private final int score;

    public Team(String name, int score) {

        this.name = name;
        this.score = score;
    }

    public String getName() {
        return name;
    }

    public int getScore() {
        return score;
    }
}
