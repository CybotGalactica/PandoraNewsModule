package org.simonscode.telegrambots.framework.modules.pandoratracker;

import java.io.Closeable;
import java.io.File;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class Database implements Closeable {
    private final PandoraTracker tracker;
    private Connection connection;
    private PreparedStatement insertMessage;
    private PreparedStatement insertCurrentScore;
    private PreparedStatement insertKill;
    private PreparedStatement insertPuzzle;
    private PreparedStatement getTeamIdByName;
    private PreparedStatement getTeamAliasById;
    private PreparedStatement getAliasByFullName;
    private PreparedStatement getUserIdToAliases;

    public Database(PandoraTracker tracker) {
        this.tracker = tracker;
        try {
            boolean dbExists = new File("database.sqlite").exists();
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:database.sqlite");
            connection.setAutoCommit(true);
            if (!dbExists) {
                System.err.println("NO DATABASE!!!");
                System.exit(-1);
            }
            insertMessage = connection.prepareStatement("INSERT INTO messages (message) VALUES (?);");
            insertCurrentScore = connection.prepareStatement("INSERT INTO scores (team, score) VALUES (?, ?);");
            insertKill = connection.prepareStatement("INSERT INTO kills (killer, victim) VALUES (?, ?);");
            insertPuzzle = connection.prepareStatement("INSERT INTO puzzels (player, puzzle) VALUES (?, ?);");
            getTeamIdByName = connection.prepareStatement("SELECT id FROM teams WHERE fullName = ?;");
            getTeamAliasById = connection.prepareStatement("SELECT alias FROM teams WHERE id = ?;");
            getAliasByFullName = connection.prepareStatement("SELECT alias FROM teams WHERE fullName = ? LIMIT 1;");
            getUserIdToAliases = connection.prepareStatement("SELECT userId, alias FROM teams;");
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
            tracker.debug("Error: " + e.getMessage());
        }
    }

    public void insertCurrectScore(String teamName, int score) {
        try {
            insertCurrentScore.setString(1, teamName);
            insertCurrentScore.setInt(2, score);
            insertCurrentScore.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("Duplicates")
    public void insertKill(String killer, String victim) {
        try {
            insertKill.setString(1, killer);
            insertKill.setString(2, victim);
            insertKill.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("Duplicates")
    public void insertPuzzle(String player, String puzzle) {
        try {
            insertPuzzle.setString(1, player);
            insertPuzzle.setString(2, puzzle);
            insertPuzzle.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public int getTeamIdByFullName(String teamName) throws SQLException {
        getTeamIdByName.setString(1, teamName);
        ResultSet resultSet = getTeamIdByName.executeQuery();
        resultSet.first();
        return resultSet.getInt(1);
    }

    public String getTeamAliasById(int id) throws SQLException {
        getTeamAliasById.setInt(1, id);
        ResultSet resultSet = getTeamAliasById.executeQuery();
        resultSet.first();
        return resultSet.getString(1);
    }

    public String getAliasByFullName(String fullName) throws SQLException {
        getAliasByFullName.setString(1, fullName);
        ResultSet resultSet = getAliasByFullName.executeQuery();
        resultSet.first();
        return resultSet.getString(1);
    }

    public Map<Integer, String> getUserIdToAliases() throws SQLException {
        ResultSet resultSet = getUserIdToAliases.executeQuery();
        Map<Integer, String> aliases = new HashMap<>();
        while (resultSet.next()) {
            aliases.put(resultSet.getInt("userId"), resultSet.getString("alias"));
        }
        return aliases;
    }

    @Override
    public void close() {
        try {
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
            tracker.debug("Error closing database! " + e.getMessage());
        }
    }
}
