package org.simonscode.telegrambots.framework.modules.pandoratracker;

public class Team implements Comparable {
    private OldPandoraBot pandoraTracker;
    private String name;
    private int score;
    private int rank;

    public Team(OldPandoraBot pandoraTracker, String name, int score) {
        this.pandoraTracker = pandoraTracker;
        this.name = name;
        this.score = score;
    }

    public int getRank() {
        return rank;
    }

    public void setRank(int rank) {
        this.rank = rank;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public void save() {
//        try {
//            pandoraTracker.insertStatement.setString(1, name);
//            pandoraTracker.insertStatement.setInt(2, score);
//            pandoraTracker.insertStatement.setInt(3, rank);
//            pandoraTracker.insertStatement.setLong(4, System.currentTimeMillis());
//            pandoraTracker.insertStatement.executeUpdate();
//        } catch (SQLException e) {
//            e.printStackTrace();
//        }
    }

    @Override
    public int compareTo(Object other) {
        if (other == null)
            return 0;
        if (!(other instanceof Team)) {
            return 0;
        }
        return ((Team) other).getScore() - score;
    }
}
