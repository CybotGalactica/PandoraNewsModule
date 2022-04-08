package org.cybotgalactica.pandoratracker;

import org.cybotgalactica.pandoratracker.bots.DiscordBot;
import org.cybotgalactica.pandoratracker.bots.TelegramBot;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@Service
public class Runner {
    public Runner(StompBroker broker,
                  @Value("${tracker.official:false}") boolean isOfficial,
                  @Value("${token.telegram:}") String telegramToken,
                  @Value("${token.discord:}") String discordToken) {
        PandoraTracker pandoraTracker = new PandoraTracker(isOfficial);

        // Stomp Broker
        broker.setMessageHandler(pandoraTracker::onUpdate);

        MessageConsumer botDebugConsumer = (m) -> pandoraTracker.queueDebug(m.getText());

        // Telegram
        if (telegramToken != null && !telegramToken.equals("")) {
            try {
                TelegramBotsApi api = new TelegramBotsApi(DefaultBotSession.class);
                TelegramBot telegramBot = new TelegramBot(telegramToken, isOfficial, pandoraTracker, botDebugConsumer);
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
    }
}
