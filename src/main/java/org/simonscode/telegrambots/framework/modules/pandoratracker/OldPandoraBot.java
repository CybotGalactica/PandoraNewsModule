package org.simonscode.telegrambots.framework.modules.pandoratracker;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.simonscode.telegrambots.framework.Bot;
import org.telegram.telegrambots.api.methods.ParseMode;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.api.objects.Message;
import org.telegram.telegrambots.exceptions.TelegramApiException;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

public class OldPandoraBot {
    private static final long CHATID = -1001050885996L;
    private PreparedStatement insertMessage;
    private long targetChannel = 282400797L;
    //    private String targetChannel = "@pandonews";

    private PreparedStatement insertStatement;
    private PreparedStatement getBetweenTimePoints;
    private PreparedStatement getPointsPerTeam;
    private Thread child;
    private LinkedBlockingQueue<Integer> messageQueue;
    private HashMap<Integer, Message> messages;
    private boolean isRunning = true;
    private State state = new State();
    private Bot bot;
    private OldPandoraBot(Bot bot) {
        this.bot = bot;
//        insertStatement = connection.prepareStatement("INSERT INTO timepoints (TeamName, Score, Rank, TimeCode) VALUES (?, ?, ?,?);");
//        getBetweenTimePoints = connection.prepareStatement("SELECT  TeamName,  Score,  TimeCode FROM timepoints;");
//        getPointsPerTeam = connection.prepareStatement("SELECT Score, TimeCode FROM timepoints WHERE TeamName = ?;");

        messageQueue = new LinkedBlockingQueue<>();
        messages = new HashMap<>();
        child = new Thread(this::setupRegularTask);
        child.setDaemon(true);
        child.setName("Lucifer");
    }


    private void setupRegularTask() {
        try {
            sendNewsMessage();
            ArrayList<String> updates = new ArrayList<>();
            for (int i = 0;isRunning; i++) {
                pullLiveUpdates(updates);
                Thread.sleep(5000);
                if (i % 5 == 0) {
                    try {
                        if (messageQueue.size() > 3) {
                            editNewsMessage();
                        }
                        if (updates.size() > 0) {
                            SendMessage msg = new SendMessage();
                            msg.setChatId(targetChannel);
                            msg.setText(String.join("", updates));
                            updates.clear();
                            if (bot != null) {
                                Message message = bot.execute(msg);
                                messageQueue.put(message.getMessageId());
                                messages.put(message.getMessageId(), message);
                            }
                            System.out.println(msg.getText());
                        }
                    } catch (TelegramApiException | IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (InterruptedException | TelegramApiException | IOException e) {
            e.printStackTrace();
        }
    }

    private void sendNewsMessage() throws IOException, TelegramApiException, InterruptedException {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(targetChannel);
        sendMessage.setParseMode(ParseMode.MARKDOWN);
        sendMessage.setText(generateScoreboard());
        if (bot != null) {
            messageQueue.put(bot.execute(sendMessage).getMessageId());
        }
    }

    private void editNewsMessage() throws IOException {
        Integer textId = messageQueue.poll();
        Integer newsId = messageQueue.peek();

        EditMessageText editText = new EditMessageText();
        editText.setChatId(targetChannel);
        editText.setMessageId(textId);
        editText.setText(messages.get(newsId).getText());


        EditMessageText editNews = new EditMessageText();
        editNews.setChatId(targetChannel);
        editNews.setParseMode(ParseMode.MARKDOWN);
        editNews.setText(generateScoreboard());
        editNews.setMessageId(newsId);
        if (bot != null) {
            try {
                bot.execute(editText);
                bot.execute(editNews);
            } catch (Exception e) {
                System.err.println("Unchanged scoreboard!");
                e.printStackTrace();
            }
        }
    }

    private void pullLiveUpdates(ArrayList<String> updates) {
        try {
            Document doc = Jsoup.connect("https://iapandora.me/ajax/recentupdates.php").userAgent("pandoratracker-telegram-channel").get();
            List<News> news = getNews(doc);
            news.removeAll(state.getNews());
            if (news.size() > 0) {
                List<Change> changes = pullScoreBorad();
                updates.add(generateUpdateText(news, changes));
                state.getNews().addAll(news);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String generateUpdateText(List<News> news, List<Change> changes) {
        StringBuilder sb = new StringBuilder();
        for (News item : news) {
            if (item.getType().equals(NewsType.KILL)) {
                sb.append(item.getSrcUser());
                sb.append(" (");
                sb.append(item.getSrcTeam());
                sb.append(") \uD83D\uDD2A ");
                sb.append(item.getDstUser());
                sb.append(" (");
                sb.append(item.getDstTeam());
                sb.append(')');
            } else {
                sb.append(item.getMessage().replace("has solved", "\uD83C\uDFB2"));
            }
            for (Change change : changes) {
                if (item.getMessage().startsWith(change.getTeam().getName()) && (change.getType().equals(ChangeType.BOTH) || change.getType().equals(ChangeType.SCORE)) && change.getNewScore() > change.getOldScore()) {
                    sb.append(" gaining them ");
                    sb.append(Math.abs(change.getNewScore() - change.getOldScore()));
                    sb.append(" Points");
                    if (change.getOldRank() != change.getNewRank()) {
                        sb.append(" and Rank ");
                        sb.append(change.getNewRank());
                    }
                    sb.append('.');
                    break;
                }
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    private String generateScoreboard() throws IOException {
        Document doc = Jsoup.connect("https://iapandora.me/scores.php?dag=total").userAgent("pandoratracker-telegram-channel").get();
        List<Team> teams = getTeams(doc);
        List<Team> top = teams.subList(0, 10);

        StringBuilder sb = new StringBuilder();
        sb.append("```\n");
        int longest = 0;
        for (Team t : top) {
            if (t.getName().startsWith("Cashgrabber's"))
                t.setName("Cashgrabbers");
            if (t.getName().length() > longest)
                longest = t.getName().length();
        }
        for (int i = 0; i < top.size(); i++) {
            Team team = top.get(i);
            sb.append(i == top.size() - 1 ? "" : " ");
            sb.append(team.getRank());
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

    private List<Change> pullScoreBorad() {
        try {
            Document doc = Jsoup.connect("https://iapandora.me/scores.php?dag=total").userAgent("pandoratracker-telegram-channel").get();
            List<Team> teams = getTeams(doc);
            List<Change> changes = detectChange(state.getTreams(), teams);
            state.setTreams(teams);
            return changes;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private List<News> getNews(Document document) {
        List<News> list = new ArrayList<>();
        for (Element element : document.body().getElementsByClass("newsitem")) {
            String newsline = element.toString().split("\n")[1];
            newsline = newsline.substring(newsline.indexOf("</span> ") + 8).trim();
            String actionString = element.toString().split("\n")[4];
            NewsType type = actionString.contains("Pandora day") ? NewsType.SOLVE : NewsType.KILL;
            if (type.equals(NewsType.KILL)) {
                int fromIndex = actionString.indexOf('>');
                String srcTeamName = actionString.substring(fromIndex + 1, actionString.indexOf('<', fromIndex)).trim();
                String dstTeamName = actionString.substring(actionString.indexOf("</span>") + 8, actionString.lastIndexOf('<')).trim();
                String[] actors = newsline.split(" killed ");
                list.add(new News(newsline, srcTeamName, dstTeamName, actors[0], actors[1]));
            } else {
                list.add(new News(newsline));
            }
        }
        return list;
    }

    private List<Team> getTeams(Document document) {
        ArrayList<Team> teams = new ArrayList<>();
        Elements teamlist = document.getElementById("totaalpunten").getElementsByTag("tbody").first().children();
        for (Element listItem : teamlist) {
            Elements props = listItem.children();
            teams.add(new Team(this, props.first().text(), Integer.parseInt(props.last().text())));
        }
        Collections.sort(teams);
        for (int i = 0; i < teams.size(); i++) {
            teams.get(i).setRank(i + 1);
        }
        return teams;
    }

    private List<Change> detectChange(List<Team> oldTeams, List<Team> newTeams) {
        ArrayList<Change> changedTeams = new ArrayList<>();
        for (Team oldTeam : oldTeams) {
            for (Team newTeam : newTeams) {
                if (oldTeam.getName().equals(newTeam.getName())) {
                    if (oldTeam.getRank() != newTeam.getRank() && oldTeam.getScore() != newTeam.getScore()) {
                        changedTeams.add(new Change(newTeam, oldTeam.getRank(), newTeam.getRank(), oldTeam.getScore(), newTeam.getScore()));
                        newTeam.save();
                    } else if (oldTeam.getRank() != newTeam.getRank()) {
                        changedTeams.add(new Change(newTeam, ChangeType.RANK, oldTeam.getRank(), newTeam.getRank()));
                        newTeam.save();
                    } else if (oldTeam.getScore() != newTeam.getScore()) {
                        changedTeams.add(new Change(newTeam, ChangeType.SCORE, oldTeam.getScore(), newTeam.getScore()));
                        newTeam.save();
                    }
                }
            }
        }
        return changedTeams;
    }

}
