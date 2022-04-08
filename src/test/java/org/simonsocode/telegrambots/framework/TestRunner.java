package org.simonsocode.telegrambots.framework;

import org.cybotgalactica.pandoratracker.MessageConsumer;
import org.cybotgalactica.pandoratracker.PandoraTracker;
import org.cybotgalactica.pandoratracker.bots.DiscordBot;
import org.cybotgalactica.pandoratracker.bots.TelegramBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.util.Timer;
import java.util.TimerTask;

public class TestRunner {
    public static void main(String[] args) {
        PandoraTracker pandoraTracker = new PandoraTracker(false);

        String telegramToken = args.length >= 2 ? args[1] : null;
        String discordToken = args.length >= 3 ? args[2] : null;


        MessageConsumer botDebugConsumer = (m) -> pandoraTracker.queueDebug(m.getText());

        // Telegram
        if (telegramToken != null && !telegramToken.equals("")) {
            try {
                TelegramBotsApi api = new TelegramBotsApi(DefaultBotSession.class);
                TelegramBot telegramBot = new TelegramBot(telegramToken, false, pandoraTracker, botDebugConsumer);
                pandoraTracker.addMessageConsumer((m) -> telegramBot.sendMessage(m.getText()));
                pandoraTracker.addDebugMessageConsumer((m) -> telegramBot.sendDebug(m.getText()));
                api.registerBot(telegramBot);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("Skipping telegram init: no token given");
        }

        // Discord
        if (discordToken != null && !discordToken.equals("")) {
            DiscordBot discordBot = new DiscordBot(discordToken, botDebugConsumer);
            discordBot.preLoad();
            Runtime.getRuntime().addShutdownHook(new Thread(discordBot::postUnload));
            pandoraTracker.addMessageConsumer((m) -> discordBot.sendUpdate(m.getText()));
            pandoraTracker.addDebugMessageConsumer((m) -> discordBot.sendDebug(m.getText()));
        } else {
            System.out.println("Skipping discord init: no token given");
        }
        // Start
        pandoraTracker.start();
        new Timer().scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                pandoraTracker.onUpdate("[{ \"type\": \"kill\", \"killer\": { \"id\": 15, \"name\": \"Knaboss\", \"team\": { \"id\": 14, \"name\": \"Knaboeven\" } }, \"victim\": { \"id\": 72, \"name\": \"Obsidian\", \"team\": { \"id\": 12, \"name\": \"Diamond hoes\" } }, \"message\": \"Knaboss pwned Obsidian's head!\" }]");
                pandoraTracker.onUpdate("[{ \"type\": \"puzzle\", \"team\": { \"id\": \"Beagle Boys\", \"name\": \"Beagle Boys\" }, \"puzzle\": { \"number\": 1, \"title\": \"Climbing trees with Prof. Banana\", \"bonus\": false }, \"time_bonus\": 0, \"message\": \"Beagle Boys solved puzzle 1.\" }]");
            }
        }, 11_000L, 11_000L);
        
    }
}
