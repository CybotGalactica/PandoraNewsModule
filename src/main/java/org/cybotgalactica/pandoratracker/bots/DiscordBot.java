package org.cybotgalactica.pandoratracker.bots;

import org.cybotgalactica.pandoratracker.controller.BotControlInterface;
import org.cybotgalactica.pandoratracker.controller.PandoraTracker;
import org.cybotgalactica.pandoratracker.models.BotMessage;
import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.channel.TextChannel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class DiscordBot implements TrackerBot {

    private final BotControlInterface controlInterface;

    private final String bindingsFile;
    private final Long testChannelId;

    private boolean isTestMode;
    private final TextChannel debugChannel;
    private final TextChannel testChannel;
    private final DiscordApi api;

    public DiscordBot(PandoraTracker pandoraTracker,
                      @Value("${discord.token") String token,
                      @Value("${discord.channelBindingsFile:.bindings}") String bindingsFile,
                      @Value("${discord.testChannelId}") Long testChannelId, // 845753289793339472L
                      @Value("${discord.debugChannelId}") Long debugChannelId // 845772133660753920L
    ) {

        api = new DiscordApiBuilder()
                .setToken(token)
                .login().join();

        controlInterface = pandoraTracker.getBotRegistry().registerBot(this);
        this.bindingsFile = bindingsFile;

        if (testChannelId != null) {
            testChannel = api.getTextChannelById(testChannelId).orElse(null);
        } else {
            testChannel = null;
        }
        this.testChannelId = testChannelId;
        if (debugChannelId != null) {
            this.debugChannel = api.getTextChannelById(debugChannelId).orElse(null);
        } else {
            debugChannel = null;
        }

        registerEventListeners();
    }

    private void registerEventListeners() {
        api.addMessageCreateListener(event -> {
            String[] args = event.getMessageContent().split("\\s+");
            switch (args[0].toLowerCase(Locale.ROOT)) {
                case "!bind":
                    event.getChannel().sendMessage(onBind(args, event));
                    break;
                case "!unbind":
                    event.getChannel().sendMessage(onUnbind(args, event));
                    break;
                case "!list":
                    event.getChannel().sendMessage(onBindings(event));
                    break;
                case "!help":
                    event.getChannel().sendMessage("Available commands: !bind [channel], !unbind [channel], !list, !help\n\nMade by @Marnitan#3288 & Simon\n");
                    break;
            }
        });
    }

    @Override
    public void sendMessage(BotMessage message) {

    }

    @Override
    public void sendDebugIfAvailable(BotMessage debugMessage) {

    }

    @Override
    public void setTestMode(boolean isTestMode) {
        this.isTestMode = isTestMode;
    }
}
