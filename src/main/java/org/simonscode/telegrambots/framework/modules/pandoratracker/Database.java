package org.simonscode.telegrambots.framework.modules.pandoratracker;

import java.io.Closeable;
import java.io.File;
import java.sql.*;

public class Database implements Closeable {
    private final PandoraTracker tracker;
    private Connection connection;
    private PreparedStatement insertMessage;
    private PreparedStatement insertCurrentScore;
    private PreparedStatement insertKill;
    private PreparedStatement insertPuzzle;

    public Database(PandoraTracker tracker) {
        this.tracker = tracker;
        try {
            boolean dbExists = new File("database.sqlite").exists();
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:database.sqlite");
            connection.setAutoCommit(true);
            if (!dbExists) {
                Statement statement = connection.createStatement();
                statement.execute("CREATE TABLE messages\n" +
                                          "(\n" +
                                          "  id          INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,\n" +
                                          "  currentTime TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,\n" +
                                          "  message     TEXT                              NOT NULL\n" +
                                          ");");
                statement.execute("CREATE TABLE scores\n" +
                                          "(\n" +
                                          "  id          INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,\n" +
                                          "  currentTime TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,\n" +
                                          "  team        TEXT NOT NULL,\n" +
                                          "  score       INTEGER NOT NULL\n" +
                                          ");");
                statement.execute("CREATE TABLE kills (\n" +
                                          "  id          INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,\n" +
                                          "  currentTime TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,\n" +
                                          "  killer      TEXT NOT NULL,\n" +
                                          "  victim      TEXT NOT NULL\n" +
                                          ");");
                statement.execute("CREATE TABLE puzzels (\n" +
                                          "  id          INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,\n" +
                                          "  currentTime TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,\n" +
                                          "  player      TEXT NOT NULL,\n" +
                                          "  puzzle      TEXT NOT NULL\n" +
                                          ");");
            }
            insertMessage = connection.prepareStatement("INSERT INTO messages (message) VALUES (?);");
            insertCurrentScore = connection.prepareStatement("INSERT INTO scores (team, score) VALUES (?, ?);");
            insertKill = connection.prepareStatement("INSERT INTO kills (killer, victim) VALUES (?, ?);");
            insertPuzzle = connection.prepareStatement("INSERT INTO puzzels (player, puzzle) VALUES (?, ?);");
        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void insertMessage(String message) {
        try {
            insertMessage.setString(1, message);
            insertMessage.execute();
        } catch (SQLException e) {
            e.printStackTrace();
            tracker.sendDebug("Error: " + e.getMessage());
        }
    }

    public void insertCurrectScore(String teamName, int score) {
        try {
            insertCurrentScore.setString(1, teamName);
            insertCurrentScore.setInt(2, score);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void insertKill(String killer, String victim) {
        try {
            insertKill.setString(1, killer);
            insertKill.setString(2, victim);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void insertPuzzle(String player, String puzzle) {
        try {
            insertPuzzle.setString(1, player);
            insertPuzzle.setString(2, puzzle);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void close() {
        try {
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
            tracker.sendDebug("Error closing database! " + e.getMessage());
        }
    }
}
