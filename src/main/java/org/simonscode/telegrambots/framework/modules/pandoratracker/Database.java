package org.simonscode.telegrambots.framework.modules.pandoratracker;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class Database {
    private final PandoraTracker tracker;
    private PreparedStatement insertMessage;

    public Database(PandoraTracker tracker) {
        this.tracker = tracker;
        try {
            Connection connection = DriverManager.getConnection("jdbc:sqlite:database.sqlite");
            connection.setAutoCommit(true);
            insertMessage = connection.prepareStatement("INSERT INTO messages (message) VALUES (?);");
        } catch (SQLException e) {
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
}
