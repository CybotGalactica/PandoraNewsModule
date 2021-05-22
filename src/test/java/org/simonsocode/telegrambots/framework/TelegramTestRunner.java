package org.simonsocode.telegrambots.framework;

import org.simonscode.telegrambots.framework.Bot;
import org.cybotgalactica.pandoratracker.PandoraTrackerModule;
import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.TelegramBotsApi;
import org.telegram.telegrambots.exceptions.TelegramApiRequestException;

import java.util.Collections;

public class TelegramTestRunner {

    public static void main(String[] args) throws TelegramApiRequestException {
        ApiContextInitializer.init();
        TelegramBotsApi api = new TelegramBotsApi();
        PandoraTrackerModule pandoraTrackerModule = new PandoraTrackerModule();
        Bot bot = new Bot("Bot", args[0], Collections.singletonList(pandoraTrackerModule));
        pandoraTrackerModule.postLoad(bot);
        pandoraTrackerModule.getTracker().setOfficial(false);
        api.registerBot(bot);
    }
}
