package org.simonsocode.telegrambots.framework;

import org.cybotgalactica.pandoratracker.TelegramBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

public class TelegramTestRunner {

    public static void main(String[] args) throws TelegramApiException {
        TelegramBotsApi api = new TelegramBotsApi(DefaultBotSession.class);
        TelegramBot telegramBot = new TelegramBot(args[0]);
        telegramBot.getTracker().setOfficial(false);
        api.registerBot(telegramBot);
        telegramBot.getTracker().linkTelegramBot(telegramBot);

        telegramBot.getTracker().start();
    }
}
