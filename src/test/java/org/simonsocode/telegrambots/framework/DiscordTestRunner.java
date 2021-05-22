package org.simonsocode.telegrambots.framework;

import org.cybotgalactica.pandoratracker.PandoraTracker;
import org.cybotgalactica.pandoratracker.PandoraTrackerDiscordBot;

public class DiscordTestRunner {
    public static void main(String[] args) {
        PandoraTracker pandoraTracker = new PandoraTracker();

        // Discord
        PandoraTrackerDiscordBot discordBot = new PandoraTrackerDiscordBot(args[0], pandoraTracker, true);
        discordBot.preLoad();
        Runtime.getRuntime().addShutdownHook(new Thread(discordBot::postUnload));
        pandoraTracker.linkDiscordBot(discordBot);

        pandoraTracker.start();
    }
}
