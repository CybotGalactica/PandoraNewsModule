package org.cybotgalactica.pandoratracker.bots;

import org.cybotgalactica.pandoratracker.CommandConsumer;
import org.cybotgalactica.pandoratracker.MessageConsumer;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedQueue;

public class TelegramBot extends TelegramLongPollingBot {
    private final static String DEBUG_CHANNEL = "275942348";
    private final static String UNOFFICIAL_CHANNEL = DEBUG_CHANNEL;
    private final static String OFFICIAL_CHANNEL = "@pandonews";

    private final String token;
    private final boolean isOfficial;
    private final CommandConsumer commandConsumer;
    private final MessageConsumer debugConsumer;

    private final ConcurrentLinkedQueue<SendMessage> backlog = new ConcurrentLinkedQueue<>();
    private final Timer messageTimer = new Timer();

    public TelegramBot(String token, boolean isOfficial, CommandConsumer commandConsumer, MessageConsumer debugConsumer) {
        this.token = token;
        this.isOfficial = isOfficial;
        this.commandConsumer = commandConsumer;
        this.debugConsumer = debugConsumer;

        messageTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                sendBacklog();
            }
        }, 10_000, 10_000);
    }

    public void sendMessage(String message) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setText(message);
        sendMessage.setChatId(isOfficial ? OFFICIAL_CHANNEL : UNOFFICIAL_CHANNEL);
        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
            debugConsumer.consumeMessage(new org.cybotgalactica.pandoratracker.models.Message(
                    String.format("Telegram bot failed to send message %s in channel %s",
                            message,
                            isOfficial ? OFFICIAL_CHANNEL : UNOFFICIAL_CHANNEL)));
            backlog.add(sendMessage);
        }
    }

    public void sendDebug(String message) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setText(message);
        sendMessage.setChatId(DEBUG_CHANNEL);
        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void sendBacklog() {
        try {
            while (!backlog.isEmpty()) {
                execute(backlog.peek());
                backlog.poll();
            }
        } catch (TelegramApiException e) {
            e.printStackTrace();
            debugConsumer.consumeMessage(new org.cybotgalactica.pandoratracker.models.Message(
                    String.format("Telegram bot failed to send backlog messages in channel %s, %d messages in backlog remaining",
                            isOfficial ? OFFICIAL_CHANNEL : UNOFFICIAL_CHANNEL,
                            backlog.size())));
        }
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (!update.hasMessage()) {
            return;
        }
        Message message = update.getMessage();
        if (!message.hasText() || !(message.getFrom().getUserName().equals("simon_struck") || message.getFrom().getUserName().equals("NielsOverkamp"))) {
            return;
        }
        String[] args = message.getText().split("\\s", 2);
        if (args.length == 0) {
            return;
        }
        String command = args[0];
        if (command.contains("@")) {
            command = command.substring(1, command.indexOf("@"));
        }
        switch (command) {
            case "channel":
                //TODO BIND
                break;
            case "close":
                commandConsumer.close();
                break;
            case "grouping":
                commandConsumer.toggleGrouping();
                break;
            case "debuggrouping":
                commandConsumer.toggleDebugGrouping();
                break;
            case "say":
                if (args.length == 2) {
                    commandConsumer.say(args[1]);
                }
                break;
            default:
                System.out.println(update);
                break;
        }
    }

    @Override
    public String getBotUsername() {
        return "PandoraNewsBot";
    }

    @Override
    public String getBotToken() {
        return token;
    }

}
