package org.cybotgalactica.pandoratracker;

import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.server.Server;
import org.javacord.api.event.message.MessageCreateEvent;

import java.util.*;
import java.util.stream.Collectors;

public class PandoraTrackerDiscordBot {

    private final Map<Long, TextChannel> bindings = new HashMap<Long, TextChannel>();

    public PandoraTrackerDiscordBot(String token) {
        DiscordApi api = new DiscordApiBuilder()
                .setToken(token)
                .login().join();

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

    private String onBind(String[] args, MessageCreateEvent event) {
        if (args.length > 1) {
            Optional<Server> maybeServer = event.getServer();
            if (maybeServer.isPresent()) {
                Server server = maybeServer.get();
                List<ServerTextChannel> channels = server.getTextChannelsByName(args[1]);
                if (channels.size() < 1) {
                    channels = server.getTextChannelsByNameIgnoreCase(args[1]);
                    if (channels.size() < 1) {
                        return String.format("Could not find a channel named '%s'", args[1]);
                    }
                }
                ServerTextChannel channel = channels.get(0);
                if (bindings.containsKey(channel.getId())) {
                    return String.format("Already bound to channel #%s", channel.getName());
                }
                bindings.put(channel.getId(), channel);
                return String.format("Bound PandoraNewsBot to channel #%s", channel.getName());
            } else {
                return "Can only bind to named channel if in a server";
            }
        } else {
            TextChannel channel = event.getChannel();
            Optional<ServerTextChannel> optionalServerTextChannel = channel.asServerTextChannel();
            if (bindings.containsKey(channel.getId())) {
                if (optionalServerTextChannel.isPresent()) {
                    return String.format("Already bound to channel #%s", optionalServerTextChannel.get().getName());
                } else {
                    return "Already bound to current channel";
                }
            }

            bindings.put(channel.getId(), channel);
            if (optionalServerTextChannel.isPresent()) {
                return String.format("Bound PandoraNewsBot to channel #%s", optionalServerTextChannel.get().getName());
            } else {
                return "Bound PandoraNewsBot to current channel";
            }
        }
    }

    private String onUnbind(String[] args, MessageCreateEvent event) {
        if (args.length > 1) {
            Optional<Server> maybeServer = event.getServer();
            if (maybeServer.isPresent()) {
                Server server = maybeServer.get();
                List<ServerTextChannel> channels = server.getTextChannelsByName(args[1]);
                if (channels.size() < 1) {
                    channels = server.getTextChannelsByNameIgnoreCase(args[1]);
                    if (channels.size() < 1) {
                        return String.format("Could not find channel %s", args[1]);
                    }
                }
                ServerTextChannel channel = channels.get(0);
                if (bindings.containsKey(channel.getId())) {
                    bindings.remove(channel.getId());
                    return String.format("Unbound PandoraNewsBot from channel #%s", channel.getName());
                } else {
                    return String.format("PandoraNewsBot is not bound to channel #%s", channel.getName());
                }
            } else {
                return "Can only unbind named channels if in a server";
            }
        } else {
            TextChannel channel = event.getChannel();
            Optional<ServerTextChannel> optionalServerTextChannel = channel.asServerTextChannel();
            if (bindings.containsKey(channel.getId())) {
                bindings.remove(channel.getId());
                if (optionalServerTextChannel.isPresent()) {
                    return String.format("Unbound PandoraNewsBot from channel #%s", optionalServerTextChannel.get().getName());
                } else {
                    return "Unbound PandoraNewsBot from current channel";
                }
            } else {
                if (optionalServerTextChannel.isPresent()) {
                    return String.format("PandoraNewsBot is not bound to channel #%s", optionalServerTextChannel.get().getName());
                } else {
                    return "PandoraNewsBot is not bound to current channel";
                }
            }
        }
    }

    private String onBindings(MessageCreateEvent event) {
        return "PandoraNewsBot is currently bound to these channels: " + bindings.values().stream()
                .filter(c -> c.equals(event.getChannel()) ||
                        c.asServerTextChannel()
                                .map(sc -> event.getServer().map(s -> sc.getServer().equals(s)).orElse(false))
                                .orElse(false))
                .map(c -> c.asServerTextChannel().map(sc -> "#" + sc.getName()).orElse("this channel"))
                .collect(Collectors.joining(", "));
    }

    public void sendUpdate(String string) {
        bindings.values().forEach(c -> c.sendMessage(string));
    }
}
