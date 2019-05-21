package org.simonscode.telegrambots.framework.modules.pandoratracker;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.simonscode.telegrambots.framework.Bot;
import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.TelegramBotsApi;
import org.telegram.telegrambots.api.methods.ParseMode;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.exceptions.TelegramApiException;
import org.telegram.telegrambots.exceptions.TelegramApiRequestException;

import java.io.IOException;
import java.util.Collections;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedQueue;

public class PandoraTracker {
    private final String debugChannel = "-1001405087548";
    private final String unofficialChannel = debugChannel;
    private final String officialChannel = "@pandonews";
    private final ConcurrentLinkedQueue<Update> messageQueue = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<String> debugQueue = new ConcurrentLinkedQueue<>();
    private final long timeBetweenMessages = 15_000;
    private final Timer messageTimer = new Timer();
    private boolean isOfficial = false;
    private Bot bot;
    private Database db;
    private WSClient wsFeed;
    private Scoreboard scoreboard;
    private Gson gson;

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
        wsFeed = new WSClient(this, "wss://www.iapandora.nl/ws/pandora", this::onUpdate);
        scoreboard = new Scoreboard();
        gson = new GsonBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .create();

        messageTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                sendMessageIfNeeded();
            }
        }, timeBetweenMessages, timeBetweenMessages);

        debug("Bot is up and running!");
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
            sendUpdate(sb.toString(), false);
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

    void debug(String debugMessage) {
        debugQueue.add("[Debug] " + debugMessage);
    }

    private void sendUpdate(String message, boolean markdown) {
        try {
            SendMessage sendMessage = new SendMessage();
            sendMessage.setText(message);
            if (markdown) {
                sendMessage.setParseMode(ParseMode.MARKDOWN);
            }
            sendMessage.setChatId(isOfficial ? officialChannel : unofficialChannel);
            bot.execute(sendMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
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

    public void stop() {
        db.close();
    }

    private void onUpdate(String updateMessage) {
        if (updateMessage.equals("--heartbeat--")) {
            return;
        }
        Update update = gson.fromJson(updateMessage, Update.class);
        System.out.println("Received: " + update);
        db.insertMessage(updateMessage);
        messageQueue.add(update);
    }

    void toggleOfficial() {
        isOfficial = !isOfficial;
        debug("Switched to " + (isOfficial ? "Official" : "Debug"));
    }

    void postScoreboard() {
        if (scoreboard == null) {
            sendDebug("Not initialized, yet!");
            return;
        }
        try {
            sendUpdate(scoreboard.fetchScoreBoardMessage(), true);
            sendDebug("Sent Scoreboard!");
        } catch (IOException e) {
            e.printStackTrace();
            sendDebug("Sending Scoreboard failed! " + e.getMessage());
        }
    }
}
