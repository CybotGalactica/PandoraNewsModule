package org.cybotgalactica.pandoratracker;

import org.simonscode.telegrambots.framework.Bot;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.TelegramBotsApi;
import org.telegram.telegrambots.exceptions.TelegramApiRequestException;

import java.util.Collections;

@Service
public class Runner {
    public Runner(StompBroker broker,
                  @Value("${token.telegram:}") String telegramToken,
                  @Value("${token.discord:}") String discordToken) {
        PandoraTracker pandoraTracker = new PandoraTracker();
        pandoraTracker.setOfficial(true);

        // Stomp Broker
        broker.setMessageHandler(pandoraTracker::onUpdate);

        // Telegram
        if (telegramToken != null && !telegramToken.equals("")) {
            try {
                ApiContextInitializer.init();
                TelegramBotsApi api = new TelegramBotsApi();
                PandoraTrackerModule pandoraTrackerModule = new PandoraTrackerModule(pandoraTracker);
                Bot bot = new Bot("Bot", telegramToken, Collections.singletonList(pandoraTrackerModule));
                pandoraTrackerModule.postLoad(bot);
                api.registerBot(bot);
            } catch (TelegramApiRequestException e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("Skipping telegram init: no token given");
        }
        
        // Discord
        if (discordToken != null && !discordToken.equals("")) {
            PandoraTrackerDiscordBot discordBot = new PandoraTrackerDiscordBot(discordToken, pandoraTracker);
            discordBot.preLoad();
            Runtime.getRuntime().addShutdownHook(new Thread(discordBot::postUnload));
            pandoraTracker.linkDiscordBot(discordBot);
        } else {
            System.out.println("Skipping discord init: no token given");
        }

        // Start
        pandoraTracker.start();
    }
}
