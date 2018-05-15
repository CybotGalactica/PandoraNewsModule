package org.simonscode.telegrambots.framework.modules.pandoratracker;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

public class Scoreboard {
    private static final String URL = "https://www.iapandora.nl/scores";
    private final Gson gson;
    private Map<Integer, String> userIdToAlias;
    private Map<String, String> fullNameToAlias;
    private String scoreboardText;
    private Thread scoreboardFetcher;

    public Scoreboard() {
        userIdToAlias = new HashMap<>();
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

        userIdToAlias.put(2, "Meltdown 6");
        userIdToAlias.put(3, "Arstotzkaasschaaf");
        userIdToAlias.put(4, "CatalonIA");
        userIdToAlias.put(5, "Sealand");
        userIdToAlias.put(6, "U.S.S.Arrrrr");
        userIdToAlias.put(7, "Schmutzig");
        userIdToAlias.put(8, "Abusement World");
        userIdToAlias.put(9, "San Quentin");
        userIdToAlias.put(10, "Carpe Noctem");
        userIdToAlias.put(11, "Roast Mobsters");
        userIdToAlias.put(13, "Easteros");
        userIdToAlias.put(14, "Radio-Actief");
        userIdToAlias.put(15, "New Nippal");
        userIdToAlias.put(16, "Teringtubbieland");
        userIdToAlias.put(17, "Sherlockington");
        userIdToAlias.put(18, "Brakfrika");
        userIdToAlias.put(19, "Kapitalipsum");
        userIdToAlias.put(20, "West Korea");
        userIdToAlias.put(21, "Assgard");
        userIdToAlias.put(22, "DPR of IAPC");
        userIdToAlias.put(23, "Survivors Guide");
        userIdToAlias.put(24, "Rainbow mutations");
        userIdToAlias.put(25, "Tegijl");
        userIdToAlias.put(26, "Team UnBEETable");
        userIdToAlias.put(27, "Black beads");
        userIdToAlias.put(28, "Disneyland");
    }

    public String getText() {
        if (scoreboardText == null) {
            System.out.println("This should run once.");
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
            HttpClient client = HttpClients.createDefault();
            HttpGet get = new HttpGet(URL);
            HttpResponse response = client.execute(get);
            HttpEntity entity = response.getEntity();
            BufferedReader br = new BufferedReader(new InputStreamReader(entity.getContent()));
            String json = "";
            while (br.ready()) {
                String line = br.readLine().trim();
                if (line.startsWith("data: [{")) {
                    json = line;
                    break;
                }
            }
            String substring = json.substring(6, json.length() - 1);
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
