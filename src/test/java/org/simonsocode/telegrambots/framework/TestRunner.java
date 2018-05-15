package org.simonsocode.telegrambots.framework;

import org.simonscode.telegrambots.framework.Bot;
import org.simonscode.telegrambots.framework.modules.pandoratracker.PandoraTrackerModule;
import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.TelegramBotsApi;
import org.telegram.telegrambots.exceptions.TelegramApiRequestException;

import java.util.Collections;

public class TestRunner {

    private static final String testBotUsername = "Simon\\u0027s Bot";
    private static final PandoraTrackerModule testModule = new PandoraTrackerModule();

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
