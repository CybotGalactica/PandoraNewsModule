package org.simonscode.telegrambots.framework.modules.pandoratracker;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.sql.DriverManager;
import java.util.*;

public class Scoreboard {
    private static final int startingDayOfYear = 134;
    private static final String URL_BASE = "https://iapandora.nl/scores/day";
    private final Gson gson;
    private Timer timer;
    private Database db;
    private Map<Integer, String> aliases;

    private static String getUrl() {
        Calendar calendar = Calendar.getInstance();
        int dayOfYear = calendar.get(Calendar.DAY_OF_YEAR);
        return URL_BASE + Integer.toString(dayOfYear - startingDayOfYear + 1);
    }

    public Scoreboard(Database db) {

        gson = new GsonBuilder().create();
    }

    public void start() {
        if (timer == null) {
            timer = new Timer();
            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    sendScoreboard();
                }
            }, 0, 5000L);
        }
    }

    public void stop() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    private void sendScoreboard() {
        try {
            String json = Jsoup.connect(getUrl())
                    .ignoreContentType(true)
                    .execute()
                    .body();
            List<ScoreboardRow> scores = parse(json);
            generate(scores);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private List<ScoreboardRow> parse(String json) {
        return Arrays.asList(gson.fromJson(json, ScoreboardRow[].class));
    }

    String generate(List<ScoreboardRow> scores) {
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
            sb.append(i == scores.size() - 1 ? "" : " ");
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
    }
}
