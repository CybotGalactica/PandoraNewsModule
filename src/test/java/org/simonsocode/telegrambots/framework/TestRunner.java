package org.simonsocode.telegrambots.framework;

import org.simonscode.telegrambots.framework.Bot;
import org.cybotgalactica.pandoratracker.PandoraTracker;
import org.cybotgalactica.pandoratracker.PandoraTrackerDiscordBot;
import org.cybotgalactica.pandoratracker.PandoraTrackerModule;
import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.TelegramBotsApi;
import org.telegram.telegrambots.exceptions.TelegramApiRequestException;

import java.util.Collections;
import java.util.Timer;
import java.util.TimerTask;

public class TestRunner {
    public static void main(String[] args) throws TelegramApiRequestException {
        PandoraTracker pandoraTracker = new PandoraTracker();
        pandoraTracker.setOfficial(false);

        // Telegram
        ApiContextInitializer.init();
        TelegramBotsApi api = new TelegramBotsApi();
        PandoraTrackerModule pandoraTrackerModule = new PandoraTrackerModule(pandoraTracker);
        Bot bot = new Bot("Bot", args[0], Collections.singletonList(pandoraTrackerModule));
        pandoraTrackerModule.postLoad(bot);
        api.registerBot(bot);

        // Discord
        PandoraTrackerDiscordBot discordBot = new PandoraTrackerDiscordBot(args[1], pandoraTracker);
        discordBot.preLoad();
        Runtime.getRuntime().addShutdownHook(new Thread(discordBot::postUnload));
        pandoraTracker.linkDiscordBot(discordBot);

        // Start
        pandoraTracker.start();
        new Timer().scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                pandoraTracker.onUpdate("{ \"type\": \"kill\", \"killer\": { \"id\": 15, \"name\": \"Knaboss\", \"team\": { \"id\": 14, \"name\": \"Knaboeven\" } }, \"victim\": { \"id\": 72, \"name\": \"Obsidian\", \"team\": { \"id\": 12, \"name\": \"Diamond hoes\" } }, \"message\": \"Knaboss pwned Obsidian's head!\" }");
                pandoraTracker.onUpdate("{ \"type\": \"puzzle\", \"team\": { \"id\": \"Beagle Boys\", \"name\": \"Beagle Boys\" }, \"puzzle\": { \"number\": 1, \"title\": \"Climbing trees with Prof. Banana\", \"bonus\": false }, \"time_bonus\": 0, \"message\": \"Beagle Boys solved puzzle 1.\" }");
            }
        }, 11_000L, 11_000L);
        
    }
}
