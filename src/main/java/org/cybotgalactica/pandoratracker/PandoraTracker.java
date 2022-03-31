package org.cybotgalactica.pandoratracker;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.cybotgalactica.pandoratracker.models.Update;
import org.simonscode.telegrambots.framework.Bot;
import org.telegram.telegrambots.api.methods.ParseMode;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.api.objects.Message;
import org.telegram.telegrambots.exceptions.TelegramApiException;

import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

public class PandoraTracker {
    @SuppressWarnings("FieldCanBeLocal")
    private final String debugChannel = "275942348";
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
    private PandoraTrackerDiscordBot discordBot;
    private Database db;
    private boolean enableScoreboard = false;
    private Scoreboard scoreboard;
    private Gson gson;

    public void start() {
        db = new Database(this);
        if (enableScoreboard) {
            scoreboard = new Scoreboard();
        }
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
                if (enableScoreboard) {
                    postScoreboard();
                }
            }
        }, 3_000);
    }

    public void linkTelegramBot(Bot bot) {
        this.bot = bot;
    }

    public void linkDiscordBot(PandoraTrackerDiscordBot discordBot) {
        this.discordBot = discordBot;
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

            if (history.size() > 3 && bot != null && enableScoreboard) {
                updateScoreboard(history.poll());
            }
        }
        if (!debugQueue.isEmpty() && bot != null) {
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
        System.out.printf("[Debug] %s\n", debugMessage);
        if (bot == null) {
            System.out.println("Could not send debug message: telegrambot offline");
            return;
        }
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(debugChannel);
        sendMessage.setText(debugMessage);
        try {
            bot.execute(sendMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
        if (discordBot != null) {
            discordBot.sendDebug(debugMessage);
        }
    }

    void postScoreboard() {
        if (scoreboard == null || bot == null) {
            debug("[scoreboard] Not initialized, yet!");
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

    public void sendUpdate(String message) {
        if (bot != null) {
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
        if (discordBot != null) {
            discordBot.sendUpdate(message);
        }
    }

    private void updateScoreboard(Message message) {
        if (scoreboard == null || bot == null) {
            debug("Not initialized, yet!");
            return;
        }
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

    public void onUpdate(String updateMessage) {
        Type updateList = new TypeToken<ArrayList<Update>>() {}.getType();
        db.insertMessage(updateMessage);
        List<Update> update = gson.fromJson(updateMessage, updateList);
        messageQueue.addAll(update);
        System.out.printf("Received %d updates\n", update.size());
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
