package org.cybotgalactica.pandoratracker;

import org.simonscode.telegrambots.framework.Bot;
import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.TelegramBotsApi;
import org.telegram.telegrambots.exceptions.TelegramApiRequestException;

import java.util.Collections;

public class Runner {
    public static void main(String[] args) throws TelegramApiRequestException {
        PandoraTracker pandoraTracker = new PandoraTracker();
        pandoraTracker.setOfficial(true);

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
    }
}
