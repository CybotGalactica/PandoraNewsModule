package org.cybotgalactica.pandoratracker;

import java.io.Closeable;
import java.io.File;
import java.sql.*;

public class Database implements Closeable {
    private final PandoraTracker tracker;
    private Connection connection;
    private PreparedStatement insertMessage;

    Database(PandoraTracker tracker) {
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
                                          "  id          INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,\n" +
                                          "  currentTime TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,\n" +
                                          "  message     TEXT    NOT NULL\n" +
                                          ");\n" +
                                          "\n");
                System.err.println("Database was missing. Created one.");
            }
            insertMessage = connection.prepareStatement("INSERT INTO messages (message) VALUES (?);");
        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    void insertMessage(String message) {
        try {
            insertMessage.setString(1, message);
            insertMessage.execute();
        } catch (SQLException e) {
            e.printStackTrace();
            tracker.debug("Error: " + e.getMessage());
        }
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
