package org.cybotgalactica.pandoratracker;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.util.*;

class Scoreboard {
    private static final String URL = "https://www.iapandora.nl/scores";
    private final Gson gson;
    private Map<String, String> fullNameToAlias;
    private String scoreboardText;
    private Thread scoreboardFetcher;

    Scoreboard() {
        fullNameToAlias = new HashMap<>();
        gson = new GsonBuilder()
                       .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                       .create();

        fullNameToAlias.put("Meltdown 6", "Meltdown 6");
        fullNameToAlias.put("Arstotzkaasschaaf", "Arstotzkaasschaaf");
        fullNameToAlias.put("CatalonIA", "CatalonIA");
        fullNameToAlias.put("Sealand", "Sealand");
        fullNameToAlias.put("U.S.S.Arrrrr", "U.S.S.Arrrrr");
        fullNameToAlias.put("Schmutzig - K*****gezwellig", "Schmutzig");
        fullNameToAlias.put("Abusement World", "Abusement World");
        fullNameToAlias.put("San Quentin", "San Quentin");
        fullNameToAlias.put("Carpe Noctem", "Carpe Noctem");
        fullNameToAlias.put("Vaulting Roast Mobsters", "Roast Mobsters");
        fullNameToAlias.put("Easteros", "Easteros");
        fullNameToAlias.put("Radio-/Actief/", "Radio-Actief");
        fullNameToAlias.put("The Kingdom of New Nippal", "New Nippal");
        fullNameToAlias.put("Teringtubbieland", "Teringtubbieland");
        fullNameToAlias.put("Sherlockington", "Sherlockington");
        fullNameToAlias.put("Brakfrika", "Brakfrika");
        fullNameToAlias.put("Kapitalipsum", "Kapitalipsum");
        fullNameToAlias.put("West Korea", "West Korea");
        fullNameToAlias.put("Assgard", "Assgard");
        fullNameToAlias.put("Democratic Peoples Republic of IAPC", "DPR of IAPC");
        fullNameToAlias.put("Wasteland Survivor&#x27;s Guide to the Post-Apocalypse", "Survivors Guide");
        fullNameToAlias.put("Rainbow mutations", "Rainbow mutations");
        fullNameToAlias.put("Tegijl", "Tegijl");
        fullNameToAlias.put("Team UnBEETable", "Team UnBEETable");
        fullNameToAlias.put("Black beads of the yellow sun", "Black beads");
        fullNameToAlias.put("Disneyland", "Disneyland");
    }

    String getText() {
        if (scoreboardText == null) {
            fetchScoreboard();
        }
        if (scoreboardFetcher == null) {
            scoreboardFetcher = new Thread(this::fetchScoreboard);
            scoreboardFetcher.setDaemon(true);
            scoreboardFetcher.start();
        }
        return scoreboardText;
    }

    private void fetchScoreboard() {
        try {
            Document doc = Jsoup.connect(URL).get();
            String json = doc.getElementsByClass("content").first().getElementsByTag("script").first().data();
            int start = json.indexOf("data: [{");
            int end = json.indexOf("\n", start);
            String substring = json.substring(start + 6, end - 1);
            List<ScoreboardRow> scores = parse(substring);
            scores.sort(Comparator.comparingInt(ScoreboardRow::getRank));

            scoreboardText = generate(scores.subList(0, 10));
        } catch (IOException e) {
            e.printStackTrace();
        }
        scoreboardFetcher = null;
    }

    private List<ScoreboardRow> parse(String json) {
        return Arrays.asList(gson.fromJson(json, ScoreboardRow[].class));
    }

    private String generate(List<ScoreboardRow> scores) {
        StringBuilder sb = new StringBuilder();
        sb.append("```\n");
        int longest = 17;
        for (int i = 0; i < scores.size(); i++) {
            ScoreboardRow score = scores.get(i);
            String alias = fullNameToAlias.get(score.fullName);
            sb.append(i + 1 > 9 ? "" : " ");
            sb.append(i + 1);
            sb.append(" | ");
            sb.append(alias);
            for (int j = 0; j < longest - alias.length(); j++) {
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

        @SuppressWarnings("unused")
        public int getHints() {
            return hints;
        }

        @SuppressWarnings("unused")
        public String getFullName() {
            return fullName;
        }

        @SuppressWarnings("unused")
        public int getTotal() {
            return total;
        }

        @SuppressWarnings("unused")
        public String getPuzzleNumbers() {
            return puzzleNumbers;
        }

        @SuppressWarnings("unused")
        public int getUserId() {
            return userId;
        }

        @SuppressWarnings("unused")
        public int getKills() {
            return kills;
        }

        @SuppressWarnings("unused")
        public int getPuzzlePoints() {
            return puzzlePoints;
        }

        int getRank() {
            return rank;
        }

        @SuppressWarnings("unused")
        public int getTimeBonus() {
            return timeBonus;
        }
    }
}
