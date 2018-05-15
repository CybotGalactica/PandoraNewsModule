package org.simonscode.telegrambots.framework.modules.pandoratracker;

import org.simonscode.telegrambots.framework.Bot;
import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.TelegramBotsApi;
import org.telegram.telegrambots.api.methods.ParseMode;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.api.objects.Message;
import org.telegram.telegrambots.exceptions.TelegramApiException;
import org.telegram.telegrambots.exceptions.TelegramApiRequestException;

import java.io.IOException;
import java.util.Collections;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedQueue;

public class PandoraTracker {
    private final String debugChannel = "-1001334509240";
    private final String unofficialChannel = "-1001334509240";
    private final String officialChannel = "@pandonews";
    private final ConcurrentLinkedQueue<Update> messageQueue = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<Message> history = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<String> debugQueue = new ConcurrentLinkedQueue<>();
    private final long timeBetweenMessages = 10_000;
    private final Timer messageTimer = new Timer();
    private Integer scoreboardMessageId;
    private boolean isOfficial = true;
    private Bot bot;
    private Database db;
    private WSClient wsKillFeed;
    private WSClient wsKillShout;
    private WSClient wsPuzzleFeed;
    private WSClient wsNewsFeed;
    private Scoreboard scoreboard;
    private int counter = 0;

    public static void main(String[] args) throws TelegramApiRequestException {
        ApiContextInitializer.init();
        TelegramBotsApi api = new TelegramBotsApi();
        PandoraTrackerModule pandoraTrackerModule = new PandoraTrackerModule();
        Bot bot = new Bot("Bot", args[0], Collections.singletonList(pandoraTrackerModule));
        pandoraTrackerModule.postLoad(bot);
        pandoraTrackerModule.getTracker().isOfficial = false;
        api.registerBot(bot);
    }

    void start(Bot bot) {
        this.bot = bot;
        db = new Database(this);
        wsKillFeed = new WSClient(this, Update.Type.KILLFEED, "wss://iapandora.nl/ws/killfeed?subscribe-broadcast", this::onUpdate);
        wsKillShout = new WSClient(this, Update.Type.KILLSHOUT, "wss://iapandora.nl/ws/killshout?subscribe-broadcast", this::onUpdate);
        wsPuzzleFeed = new WSClient(this, Update.Type.PUZZLE, "wss://iapandora.nl/ws/puzzlefeed?subscribe-broadcast", this::onUpdate);
        wsNewsFeed = new WSClient(this, Update.Type.NEWS, "wss://iapandora.nl/ws/news?subscribe-broadcast", this::onUpdate);
        scoreboard = new Scoreboard();

        messageTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                sendMessageIfNeeded();
            }
        }, timeBetweenMessages, timeBetweenMessages);

        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                sendDebug(String.format("%s-bot is up and running!", isOfficial ? "Production" : "Testing"));
                postScoreboard();
            }
        }, 3_000);
    }

    private void sendMessageIfNeeded() {
        if (!messageQueue.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            while (!messageQueue.isEmpty()) {
                sb.append(Objects.requireNonNull(messageQueue.poll()).getText());
                if (!messageQueue.isEmpty()) {
                    sb.append('\n');
                }
            }
            sendUpdate(sb.toString());
            //             sendUpdate("Prop Newsmessage " + counter++);

            if (history.size() >= 3) {
                updateScoreboard(history.poll());
            }
        }
        if (!debugQueue.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            while (!debugQueue.isEmpty()) {
                sb.append(String.format("[%s] %s", isOfficial ? "Testing" : "Production", debugQueue.poll()));
                if (!debugQueue.isEmpty()) {
                    sb.append('\n');
                }
            }
            sendDebug(sb.toString());
        }
    }

    private void sendDebug(String debugMessage) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(debugChannel);
        sendMessage.setText(debugMessage);
        try {
            bot.execute(sendMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    void postScoreboard() {
        if (scoreboard == null) {
            debug("Not initialized, yet!");
            return;
        }
        try {
            SendMessage sendMessage = new SendMessage();
            sendMessage.setParseMode(ParseMode.MARKDOWN);
            sendMessage.setText(scoreboard.fetchScoreBoardMessage());
            sendMessage.setChatId(isOfficial ? officialChannel : unofficialChannel);
            scoreboardMessageId = bot.execute(sendMessage).getMessageId();
            //            debug("Sent Scoreboard!");
        } catch (IOException | TelegramApiException e) {
            e.printStackTrace();
            debug("Sending Scoreboard failed! " + e.getMessage());
        }
    }

    private void sendUpdate(String message) {
        try {
            SendMessage sendMessage = new SendMessage();
            sendMessage.setText(message);
            sendMessage.setChatId(isOfficial ? officialChannel : unofficialChannel);
            history.add(bot.execute(sendMessage));
            System.out.println("[Send] " + sendMessage.getText());
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void updateScoreboard(Message message) {
        Thread updater = new Thread(() -> {
            try {
                String scoreboardText = scoreboard.fetchScoreBoardMessage();
                EditMessageText edit = new EditMessageText();
                edit.setChatId(message.getChatId());
                edit.setMessageId(message.getMessageId());
                edit.setText(scoreboardText);
                edit.setParseMode(ParseMode.MARKDOWN);
                moveLastMessageToscoreboard(message);
                scoreboardMessageId = ((Message) bot.execute(edit)).getMessageId();
                System.out.println("[Edit] " + scoreboardText);
            } catch (IOException e) {
                debug("Error fetching scoreboard!" + e.getMessage());
                e.printStackTrace();
            } catch (TelegramApiException e) {
                debug("Error during scoreboard message migrating!" + e.getMessage());
                e.printStackTrace();
            }
        });
        updater.setDaemon(true);
        updater.start();
    }

    void debug(String debugMessage) {
        debugQueue.add(debugMessage);
    }

    private void moveLastMessageToscoreboard(Message message) {
        if (scoreboardMessageId == null) {
            return;
        }
        EditMessageText edit = new EditMessageText();
        edit.setChatId(message.getChatId());
        edit.setMessageId(scoreboardMessageId);
        edit.setText(message.getText());
        System.out.println("[Edit] " + message.getText());
        try {
            bot.execute(edit);
        } catch (TelegramApiException e) {
            debug("Error during scoreboard message migrating!" + e.getMessage());
            e.printStackTrace();
        }
    }

    public void stop() {
        wsKillFeed.close();
        wsKillShout.close();
        wsPuzzleFeed.close();
        wsNewsFeed.close();
        db.close();
    }

    private void onUpdate(Update.Type type, String update) {
        if (update.equals("--heartbeat--")) {
            return;
        }
        System.out.println("Received: " + update);
        db.insertMessage(update);
        messageQueue.add(new Update(type, update));
    }

    void toggleOfficial() {
        isOfficial = !isOfficial;
        debug("Switched to " + (isOfficial ? "Official" : "Debug"));
    }
}
