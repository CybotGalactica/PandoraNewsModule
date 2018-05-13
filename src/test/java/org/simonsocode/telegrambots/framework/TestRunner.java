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

        Bot bot = new Bot(testBotUsername, args[0], Collections.singletonList(testModule));
        testModule.preLoad(bot);
        api.registerBot(bot);
        testModule.postLoad(bot);
    }
}
