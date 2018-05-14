package org.simonscode.telegrambots.framework.modules.pandoratracker;

import org.simonscode.telegrambots.framework.Bot;
import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.TelegramBotsApi;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.exceptions.TelegramApiException;
import org.telegram.telegrambots.exceptions.TelegramApiRequestException;

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
    private final ConcurrentLinkedQueue<String> debugQueue = new ConcurrentLinkedQueue<>();
    boolean isOfficial = true;
    private long timeBetweenMessages = 5_000;
    private Timer messageTimer = new Timer();
    private Bot bot;
    private Database db;
    private WSClient wsKillFeed;
    private WSClient wsKillShout;
    private WSClient wsPuzzleFeed;
    private WSClient wsNewsFeed;

    PandoraTracker() {

    }

    public static void main(String[] args) throws TelegramApiRequestException {
        ApiContextInitializer.init();
        TelegramBotsApi api = new TelegramBotsApi();

        PandoraTracker pandoraTracker = new PandoraTracker();
        pandoraTracker.isOfficial = false;
        Bot bot = new Bot("Bot", args[0], Collections.singletonList(new PandoraTrackerModule()));
        api.registerBot(bot);
        pandoraTracker.start(bot);
    }

    void start(Bot bot) {
        this.bot = bot;
        db = new Database(this);
        wsKillFeed = new WSClient(this, Update.Type.KILLFEED, "wss://iapandora.nl/ws/killfeed?subscribe-broadcast", this::onUpdate);
        wsKillShout = new WSClient(this, Update.Type.KILLSHOUT, "wss://iapandora.nl/ws/killshout?subscribe-broadcast", this::onUpdate);
        wsPuzzleFeed = new WSClient(this, Update.Type.PUZZLE, "wss://iapandora.nl/ws/puzzlefeed?subscribe-broadcast", this::onUpdate);
        wsNewsFeed = new WSClient(this, Update.Type.NEWS, "wss://iapandora.nl/ws/news?subscribe-broadcast", this::onUpdate);

        debug("Bot is up and running!");
        messageTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                sendMessageIfNeeded();
            }
        }, timeBetweenMessages, timeBetweenMessages);
    }

    void debug(String debugMessage) {
        debugQueue.add("[Debug] " + debugMessage);
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
        }
        if (!debugQueue.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            while (!debugQueue.isEmpty()) {
                sb.append(debugQueue.poll());
                if (!debugQueue.isEmpty()) {
                    sb.append('\n');
                }
            }
            sendDebug(sb.toString());
        }
    }

    private void sendUpdate(String message) {
        try {
            SendMessage sendMessage = new SendMessage();
            sendMessage.setText(message);
            sendMessage.setChatId(isOfficial ? officialChannel : unofficialChannel);
            bot.execute(sendMessage);
        } catch (TelegramApiException e) {
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
