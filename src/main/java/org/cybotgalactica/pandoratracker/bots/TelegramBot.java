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

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Locale;
import java.util.Scanner;

public class TelegramBot extends TelegramLongPollingBot {
    private final static String DEBUG_CHANNEL = "275942348";
    private final static String TEST_CHANNEL = DEBUG_CHANNEL;
    private final static String BINDINGS_FILE = "telegram.bindings";

    private final String token;
    private final CommandConsumer commandConsumer;
    private final MessageConsumer debugConsumer;

    private String channel;

    private final ConcurrentLinkedQueue<SendMessage> backlog = new ConcurrentLinkedQueue<>();
    private final Timer messageTimer = new Timer();

    public TelegramBot(String token, boolean isOfficial, CommandConsumer commandConsumer, MessageConsumer debugConsumer) {
        this.token = token;
        this.commandConsumer = commandConsumer;
        this.debugConsumer = debugConsumer;

        try (Scanner scanner = new Scanner(new File(BINDINGS_FILE))) {
            if (scanner.hasNextLine()) {
                channel = scanner.nextLine();
            } else if (!isOfficial) {
                channel = TEST_CHANNEL;
            } else {
                debugConsumer.consumeMessage(new org.cybotgalactica.pandoratracker.models.Message(String.format("Binding file %s empty. Please use the \"channel\" command to bind to a channel", BINDINGS_FILE)));
            }
        } catch (FileNotFoundException e) {
            debugConsumer.consumeMessage(new org.cybotgalactica.pandoratracker.models.Message(String.format("Could not load bindings file %s. Please use the \"channel\" command to bind to a channel", BINDINGS_FILE)));
        }

        messageTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                sendBacklog();
            }
        }, 10_000, 10_000);
    }

    public void sendMessage(String message) {
        sendMessage(message, channel);
    }

    private void sendMessage(String message, String chatId) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setText(message);
        sendMessage.setChatId(chatId);
        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
            debugConsumer.consumeMessage(new org.cybotgalactica.pandoratracker.models.Message(
                    String.format("Telegram bot failed to send message %s in channel %s",
                            message,
                            chatId)));
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
        if (!message.hasText() || !message.getText().startsWith("/") || !(message.getFrom().getUserName().equals("simon_struck") || message.getFrom().getUserName().equals("NielsOverkamp"))) {
            return;
        }
        String[] args = message.getText().split("\\s", 2);
        if (args.length == 0) {
            return;
        }
        String command = args[0].toLowerCase(Locale.ROOT);
        if (command.contains("@")) {
            command = command.substring(1, command.indexOf("@"));
        }


        SendMessage reply = new SendMessage();
        reply.setChatId(message.getChatId().toString());

        switch (command) {
            case "/channel":
                String oldChannel = channel;
                if (args.length == 2) {
                    onBind(args[1]);
                    sendMessage(String.format("Replaced bound to %s with %s",oldChannel, args[1]), message.getChatId().toString());
                } else {
                    onBind(message.getChatId().toString());
                    sendMessage(String.format("Replaced bound to %s with current chat",oldChannel), message.getChatId().toString());
                }
                break;
            case "/close":
                commandConsumer.close();
                break;
            case "/grouping":
                commandConsumer.toggleGrouping();
                break;
            case "/debuggrouping":
                commandConsumer.toggleDebugGrouping();
                break;
            case "/say":
                if (args.length == 2) {
                    commandConsumer.say(args[1]);
                }
                break;
            default:
                System.out.println(update);
                sendMessage(String.format("Unknown command %s", command), message.getChatId().toString());
                break;
        }
    }

    private void onBind(String channel) {
        this.channel = channel;
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
