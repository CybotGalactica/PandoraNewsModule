package org.simonscode.telegrambotsframework.modules.pandoratracker;

public class Change {
    private Team team;
    private ChangeType type;
    private int oldRank;
    private int newRank;
    private int oldScore;
    private int newScore;

    public Change(Team team, ChangeType type, int oldValue, int newValue) {
        this.team = team;
        this.type = type;
        if (type.equals(ChangeType.RANK)) {
            this.oldRank = oldValue;
            this.newRank = newValue;
        } else if (type.equals(ChangeType.SCORE)) {
            this.oldScore = oldValue;
            this.newScore = newValue;
        }
    }

    public Change(Team team, int oldRank, int newRank, int oldScore, int newScore) {
        this.team = team;
        this.type = ChangeType.BOTH;
        this.oldRank = oldRank;
        this.newRank = newRank;
        this.oldScore = oldScore;
        this.newScore = newScore;
    }

    public Team getTeam() {
        return team;
    }

    public ChangeType getType() {
        return type;
    }

    public int getOldRank() {
        return oldRank;
    }

    public int getNewRank() {
        return newRank;
    }

    public int getOldScore() {
        return oldScore;
    }

    public int getNewScore() {
        return newScore;
    }
}
