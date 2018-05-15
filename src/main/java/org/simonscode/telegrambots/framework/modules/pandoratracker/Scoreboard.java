package org.simonscode.telegrambots.framework.modules.pandoratracker;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.jsoup.Jsoup;

import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

public class Scoreboard {
    private static final String URL = "https://iapandora.nl/scores/day1";
    private final Gson gson;
    private Map<Integer, String> aliases;


    public Scoreboard(PandoraTracker tracker, Database db) throws SQLException {
        try {
            aliases = db.getUserIdToAliases();
        } catch (SQLException e) {
            tracker.debug("Error: " + e.getMessage());
            throw e;
        }
        gson = new GsonBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .create();
    }

    //TODO: Change the jsoup stuff to acquire all the data
    public String fetchScoreBoardMessage() throws IOException {
        String json = Jsoup.connect(URL)
                .ignoreContentType(true)
                .execute()
                .body();
        List<ScoreboardRow> scores = parse(json);
        scores.sort(Comparator.comparingInt(ScoreboardRow::getRank));
        scores.subList(0, 10);
        return generate(scores);
    }

    private List<ScoreboardRow> parse(String json) {
        return Arrays.asList(gson.fromJson(json, ScoreboardRow[].class));
    }

    private String generate(List<ScoreboardRow> scores) {
        StringBuilder sb = new StringBuilder();
        sb.append("```\n");
        int longest = 0;
        for (ScoreboardRow s : scores) {
            if (aliases.get(s.userId).length() > longest) {
                longest = aliases.get(s.userId).length();
            }
        }
        for (int i = 0; i < scores.size(); i++) {
            ScoreboardRow score = scores.get(i);
            sb.append(i + 1 > 9 ? "" : " ");
            sb.append(i + 1);
            sb.append(" | ");
            sb.append(aliases.get(score.userId));
            for (int j = 0; j < longest - aliases.get(score.userId).length(); j++) {
                sb.append(' ');
            }
            sb.append(" | ");
            sb.append(score.total);
            sb.append('\n');
        }
        sb.append("```");
        return sb.toString();
    }

    private static class ScoreboardRow {
        int hints;
        String fullName;
        int total;
        String puzzleNumbers;
        int userId;
        int kills;
        int puzzlePoints;
        int rank;
        int timeBonus;

        public int getHints() {
            return hints;
        }

        public String getFullName() {
            return fullName;
        }

        public int getTotal() {
            return total;
        }

        public String getPuzzleNumbers() {
            return puzzleNumbers;
        }

        public int getUserId() {
            return userId;
        }

        public int getKills() {
            return kills;
        }

        public int getPuzzlePoints() {
            return puzzlePoints;
        }

        public int getRank() {
            return rank;
        }

        public int getTimeBonus() {
            return timeBonus;
        }
    }
}
