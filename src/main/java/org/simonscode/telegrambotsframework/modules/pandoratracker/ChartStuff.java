package org.simonscode.telegrambotsframework.modules.pandoratracker;

public class ChartStuff {

/*    private void generateTeams(String caption, int offset, int num) {
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

        int width = 1024; *//* Width of the image *//*
        int height = 768; *//* Height of the image *//*
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
    */
}
