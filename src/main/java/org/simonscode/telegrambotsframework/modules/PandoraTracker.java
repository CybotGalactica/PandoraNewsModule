package org.simonscode.telegrambotsframework.modules;


import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.telegram.telegrambots.api.methods.ParseMode;
import org.telegram.telegrambots.api.methods.send.SendDocument;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.methods.send.SendPhoto;
import org.telegram.telegrambots.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.api.objects.Message;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.exceptions.TelegramApiException;
import sun.security.krb5.Config;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by simon on 16.05.17.
 */
public class PandoraTracker extends org.simonscode.telegrambot.Module {
    private static final long CHATID = -1001050885996L;
    private final Thread child;
    private Bot bot;
    private PreparedStatement insertStatement;
    private PreparedStatement getBetweenTimePoints;
    private PreparedStatement getPointsPerTeam;
    private long targetChannel = 282400797L;
    //    private String targetChannel = "@pandonews";
    private LinkedBlockingQueue<Integer> messageQueue;
    private HashMap<Integer, Message> messages;
    public PandoraTracker(Bot bot) {
        super(bot);
        try {
            Connection connection = DriverManager.getConnection("jdbc:sqlite:database.sqlite");
            connection.setAutoCommit(true);
            insertStatement = connection.prepareStatement("INSERT INTO timepoints (TeamName, Score, Rank, TimeCode) VALUES (?, ?, ?,?);");
            getBetweenTimePoints = connection.prepareStatement("SELECT  TeamName,  Score,  TimeCode FROM timepoints;");
            getPointsPerTeam = connection.prepareStatement("SELECT Score, TimeCode FROM timepoints WHERE TeamName = ?;");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        this.bot = bot;
        messageQueue = new LinkedBlockingQueue<>();
        messages = new HashMap<>();
        child = new Thread(this::setupRegularTask);
        child.setDaemon(true);
        child.setName("Lucifer");
        child.start();
    }

    private void setupRegularTask() {
        try {
            sendNewsMessage();
            ArrayList<String> updates = new ArrayList<>();
            for (int i = 0; true; i++) {
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
                                Message message = bot.sendMessage(msg);
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

    public File generateChart(HashMap<String, HashMap<Long, Integer>> pointsPerTime) throws IOException {

        TimeSeriesCollection dataset = new TimeSeriesCollection();
        for (Map.Entry<String, HashMap<Long, Integer>> person : pointsPerTime.entrySet()) {
            TimeSeries stats = new TimeSeries(person.getKey());
            for (Map.Entry<Long, Integer> entry : person.getValue().entrySet()) {
                stats.addOrUpdate(new FixedMillisecond(entry.getKey()), entry.getValue());
            }
            dataset.addSeries(stats);
        }

        JFreeChart xylineChart = ChartFactory.createTimeSeriesChart(
                "Points per Team",
                "Time",
                "Points",
                dataset,
                true,
                true,
                false);

        int width = 1024; /* Width of the image */
        int height = 768; /* Height of the image */
        File lineChart = new File("chart.jpg");
        XYPlot plot = xylineChart.getXYPlot();
        DateAxis xAxis1 = (DateAxis) plot.getDomainAxis();
        xAxis1.setDateFormatOverride(new SimpleDateFormat("d'th' HH:mm "));
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
        renderer.setStroke(new BasicStroke(2f));
//        renderer.setSeriesPaint(0, Color.BLUE);
//        renderer.setSeriesPaint(1, Color.RED);
//        renderer.setSeriesPaint(2, Color.MAGENTA);
//        renderer.setSeriesPaint(3, Color.CYAN);
//        renderer.setSeriesPaint(4, Color.green);
//        renderer.setSeriesPaint(5, Color.ORANGE);
        renderer.setShapesVisible(false);
        plot.setRenderer(renderer);

        BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(lineChart));

        BufferedImage image = new BufferedImage(width, height, 1);
        Graphics2D g2 = image.createGraphics();
        plot.render(g2, new Rectangle2D.Double(0.0D, 0.0D, (double) width, (double) height), 0, null, null);
        g2.dispose();

        EncoderUtil.writeBufferedImage(image, "jpeg", out);

        ChartUtilities.saveChartAsJPEG(lineChart, xylineChart, width, height);
        return lineChart;
    }

    @Override
    public void processUpdate(Bot sender, Update update) {
        Message message = Utils.checkForCommand(update, "/done");
        if (message != null && message.getFrom().getFirstName().startsWith("Simon")) {
            System.out.print("Generating...");
            generatEndReport();
            System.out.println("Done!");
        }
        System.out.println(update);
    }

    private void generateTeams(String caption, int offset, int num) {
        try {
            HashMap<String, HashMap<Long, Integer>> pointsPerTime = new HashMap<>();
            Document doc = Jsoup.connect("https://iapandora.me/scores.php?dag=total").userAgent("pandoratracker-telegram-channel").get();
            List<Team> teams = getTeams(doc);
            List<Team> subTeams = teams.subList(offset, offset + num);
            for (Team team : subTeams) {
                getPointsPerTeam.setString(1, team.getName());
                ResultSet resultSet = getPointsPerTeam.executeQuery();
                while (resultSet.next()) {
                    if (pointsPerTime.containsKey(team.getName())) {
                        pointsPerTime.get(team.getName()).put(resultSet.getLong(2), resultSet.getInt(1));
                    } else {
                        HashMap<Long, Integer> data = new HashMap<>();
                        data.put(resultSet.getLong(2), resultSet.getInt(1));
                        pointsPerTime.put(team.getName(), data);
                    }
                }
            }
            File file = generateChart(pointsPerTime);
            bot.sendPhoto(new SendPhoto().setNewPhoto(file).setChatId(targetChannel).setCaption(caption));
        } catch (SQLException | IOException | TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void generatEndReport() {
        generateTeams("All Teams", 0, 26);
        generateTeams("Top 13", 0, 13);
        generateTeams("Flop 13", 13, 13);
        try {
            SendMessage scoreboard = new SendMessage();
            scoreboard.setText(generateScoreboard());
            scoreboard.setChatId(targetChannel);
            scoreboard.setParseMode(ParseMode.MARKDOWN);
            bot.sendMessage(scoreboard);
        } catch (TelegramApiException | IOException e) {
            e.printStackTrace();
        }
        SendDocument database = new SendDocument();
        try {
            database.setCaption("Database of all recorded scores (for people that want to make their own charts)");
            database.setChatId(targetChannel);
            database.setNewDocument(new File("./database.sqlite"));
            bot.sendDocument(database);
        } catch (NullPointerException | TelegramApiException e) {
            e.printStackTrace();
        }
        SendMessage finalMessage = new SendMessage();
        finalMessage.setText("Well played contestants!\nPandoraBot is terminating...");
        finalMessage.setChatId(targetChannel);
        try {
            bot.sendMessage(finalMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
        SendMessage actualFinalMessage = new SendMessage();
        actualFinalMessage.setText("Terminated.\nSee you next year!");
        actualFinalMessage.setChatId(targetChannel);
        try {
            bot.sendMessage(actualFinalMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.exit(0);
    }

    private void sendNewsMessage() throws IOException, TelegramApiException, InterruptedException {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(targetChannel);
        sendMessage.setParseMode(ParseMode.MARKDOWN);
        sendMessage.setText(generateScoreboard());
        if (bot != null) {
            messageQueue.put(bot.sendMessage(sendMessage).getMessageId());
        }
    }

    private void editNewsMessage() throws IOException, TelegramApiException {
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
                bot.editMessageText(editText);
                bot.editMessageText(editNews);
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
            news.removeAll(Config.getInstance().news);
            if (news.size() > 0) {
                List<Change> changes = pullScoreBorad();
                updates.add(generateUpdateText(news, changes));
                Config.getInstance().news.addAll(news);
                Config.getInstance().toFile();
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
                if (item.getMessage().startsWith(change.getTeam().getName()) && (change.getType().equals(Type.BOTH) || change.getType().equals(Type.SCORE)) && change.getNewScore() > change.getOldScore()) {
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
                t.setName("Cashgrabber's");
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
            List<Change> changes = detectChange(Config.getInstance().teams, teams);
            Config.getInstance().teams = teams;
            Config.getInstance().toFile();
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
            teams.add(new Team(props.first().text(), Integer.parseInt(props.last().text())));
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
                        changedTeams.add(new Change(newTeam, Type.RANK, oldTeam.getRank(), newTeam.getRank()));
                        newTeam.save();
                    } else if (oldTeam.getScore() != newTeam.getScore()) {
                        changedTeams.add(new Change(newTeam, Type.SCORE, oldTeam.getScore(), newTeam.getScore()));
                        newTeam.save();
                    }
                }
            }
        }
        return changedTeams;
    }

    public enum Type {
        RANK, SCORE, BOTH
    }

    public enum NewsType {
        KILL, SOLVE
    }

    public class Team implements Comparable {
        private String name;
        private int score;
        private int rank;

        public Team(String name, int score) {
            this.name = name;
            this.score = score;
        }

        public int getRank() {
            return rank;
        }

        public void setRank(int rank) {
            this.rank = rank;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getScore() {
            return score;
        }

        public void setScore(int score) {
            this.score = score;
        }

        public void save() {
            try {
                insertStatement.setString(1, name);
                insertStatement.setInt(2, score);
                insertStatement.setInt(3, rank);
                insertStatement.setLong(4, System.currentTimeMillis());
                insertStatement.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        @Override
        public int compareTo(Object other) {
            if (other == null)
                return 0;
            if (!(other instanceof Team)) {
                return 0;
            }
            return ((Team) other).getScore() - score;
        }
    }

    public class Change {
        private Team team;
        private Type type;
        private int oldRank;
        private int newRank;
        private int oldScore;
        private int newScore;

        public Change(Team team, Type type, int oldValue, int newValue) {
            this.team = team;
            this.type = type;
            if (type.equals(Type.RANK)) {
                this.oldRank = oldValue;
                this.newRank = newValue;
            } else if (type.equals(Type.SCORE)) {
                this.oldScore = oldValue;
                this.newScore = newValue;
            }
        }

        public Change(Team team, int oldRank, int newRank, int oldScore, int newScore) {
            this.team = team;
            this.type = Type.BOTH;
            this.oldRank = oldRank;
            this.newRank = newRank;
            this.oldScore = oldScore;
            this.newScore = newScore;
        }

        public Team getTeam() {
            return team;
        }

        public Type getType() {
            return type;
        }

        public int getOldRank() {
            return oldRank;
        }

        public int getNewRank() {
            return newRank;
        }

        public int getOldScore() {
            return oldScore;
        }

        public int getNewScore() {
            return newScore;
        }
    }

    public class News {
        private String message;
        private NewsType type;
        private String srcTeam;
        private String dstTeam;
        private String srcUser;
        private String dstUser;

        public News(String message) {
            this.message = message;
            type = NewsType.SOLVE;
        }

        public News(String message, String srcTeam, String dstTeam, String srcUser, String dstUser) {
            this.message = message;
            this.srcTeam = srcTeam.replace("amp;", "");
            this.dstTeam = dstTeam.replace("amp;", "");
            this.srcUser = srcUser;
            this.dstUser = dstUser;
            type = NewsType.KILL;
        }

        public String getSrcUser() {
            return srcUser;
        }

        public void setSrcUser(String srcUser) {
            this.srcUser = srcUser;
        }

        public String getDstUser() {
            return dstUser;
        }

        public void setDstUser(String dstUser) {
            this.dstUser = dstUser;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public NewsType getType() {
            return type;
        }

        public void setType(NewsType type) {
            this.type = type;
        }

        public String getSrcTeam() {
            return srcTeam;
        }

        public void setSrcTeam(String srcTeam) {
            this.srcTeam = srcTeam;
        }

        public String getDstTeam() {

            return dstTeam;
        }

        public void setDstTeam(String dstTeam) {
            this.dstTeam = dstTeam;
        }

        @Override
        public String toString() {
            return message;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            News news = (News) o;

            return message.equals(news.message);
        }

        @Override
        public int hashCode() {
            return message.hashCode();
        }
    }

}
