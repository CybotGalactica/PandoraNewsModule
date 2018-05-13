package org.simonscode.telegrambots.framework.modules.pandoratracker;

import java.util.HashMap;
import java.util.List;

public class Scoreboard {
    private static HashMap<String, Integer> scores = new HashMap<>();

    String generate(List<Team> teams) {
        StringBuilder sb = new StringBuilder();
        sb.append("```\n");
        int longest = 0;
        for (Team t : teams) {
            if (t.getName().length() > longest) {
                longest = t.getName().length();
            }
        }
        for (int i = 0; i < teams.size(); i++) {
            Team team = teams.get(i);
            sb.append(i == teams.size() - 1 ? "" : " ");
            sb.append(i + 1);
            sb.append(" | ");
            sb.append(team.getName());
            for (int j = 0; j < longest - team.getName().length(); j++) {
                sb.append(' ');
            }
            sb.append(" | ");
            sb.append(team.getScore());
            sb.append('\n');
        }
        sb.append("```");
        return sb.toString();
    }

}
