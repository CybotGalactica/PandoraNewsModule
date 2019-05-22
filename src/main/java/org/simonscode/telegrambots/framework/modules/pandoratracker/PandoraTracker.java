package org.simonscode.telegrambots.framework.modules.pandoratracker;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.simonscode.telegrambots.framework.Bot;
import org.telegram.telegrambots.api.methods.ParseMode;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.api.objects.Message;
import org.telegram.telegrambots.exceptions.TelegramApiException;

import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedQueue;

public class PandoraTracker {
    @SuppressWarnings("FieldCanBeLocal")
    private final String debugChannel = "-1001405087548";
    private final String unofficialChannel = debugChannel;
    private final String officialChannel = "@pandonews";
    private final ConcurrentLinkedQueue<Update> messageQueue = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<Message> history = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<String> debugQueue = new ConcurrentLinkedQueue<>();
    @SuppressWarnings("FieldCanBeLocal")
    private final long timeBetweenMessages = 10_000;
    private final Timer messageTimer = new Timer();
    @SuppressWarnings("FieldCanBeLocal")
    private boolean grouping = true;
    private Integer scoreboardMessageId;
    private boolean isOfficial = false;
    private Bot bot;
    private Database db;
    private WSClient wsFeed;
    private Scoreboard scoreboard;
    private Gson gson;

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
                if (grouping) {
                    sendMessageIfNeeded();
                }
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

            if (history.size() > 3) {
                updateScoreboard(history.poll());
            }
        }
        if (!debugQueue.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            while (!debugQueue.isEmpty()) {
                sb.append(String.format("[%s] %s", isOfficial ? "Production" : "Testing", debugQueue.poll()));
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
            sendMessage.setText(scoreboard.getText());
            sendMessage.setChatId(isOfficial ? officialChannel : unofficialChannel);
            scoreboardMessageId = bot.execute(sendMessage).getMessageId();
            //            debug("Sent Scoreboard!");
        } catch (TelegramApiException e) {
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
            System.out.println("[Sent] " + sendMessage.getText());
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void updateScoreboard(Message message) {
        Thread updater = new Thread(() -> {
            try {
                String scoreboardText = scoreboard.getText();
                EditMessageText edit = new EditMessageText();
                edit.setChatId(message.getChatId());
                edit.setMessageId(message.getMessageId());
                edit.setText(scoreboardText);
                edit.setParseMode(ParseMode.MARKDOWN);
                moveLastMessageToscoreboard(message);
                scoreboardMessageId = ((Message) bot.execute(edit)).getMessageId();
                System.out.println("[Edit] " + scoreboardText);
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
        System.out.println("[Edited] " + message.getText());
        try {
            bot.execute(edit);
        } catch (TelegramApiException e) {
            debug("Error during scoreboard message migrating!" + e.getMessage());
            e.printStackTrace();
        }
    }

    void stop() {
        wsFeed.close();
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
        if (!grouping) {
            sendMessageIfNeeded();
        }
    }

    void toggleOfficial() {
        isOfficial = !isOfficial;
        debug("Switched to " + (isOfficial ? "Official" : "Debug"));
    }

    void toggleGrouping() {
        grouping = !grouping;
        debug((grouping ? "Enabled" : "Disabled") + " grouping");
    }

    public void setOfficial(boolean isOfficial) {
        this.isOfficial = isOfficial;
    }
}
