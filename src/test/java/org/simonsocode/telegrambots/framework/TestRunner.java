package org.simonsocode.telegrambots.framework;

import org.cybotgalactica.pandoratracker.PandoraTracker;
import org.cybotgalactica.pandoratracker.DiscordBot;
import org.cybotgalactica.pandoratracker.TelegramBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.util.Timer;
import java.util.TimerTask;

public class TestRunner {
    public static void main(String[] args) throws TelegramApiException {
        PandoraTracker pandoraTracker = new PandoraTracker();
        pandoraTracker.setOfficial(false);

        // Telegram

        TelegramBotsApi api = new TelegramBotsApi(DefaultBotSession.class);
        TelegramBot telegramBot = new TelegramBot(pandoraTracker, args[0]);
        telegramBot.getTracker().setOfficial(false);
        api.registerBot(telegramBot);
        pandoraTracker.linkTelegramBot(telegramBot);

        // Discord
        DiscordBot discordBot = new DiscordBot(args[1], pandoraTracker, true);
        discordBot.preLoad();
        Runtime.getRuntime().addShutdownHook(new Thread(discordBot::postUnload));
        pandoraTracker.linkDiscordBot(discordBot);

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
