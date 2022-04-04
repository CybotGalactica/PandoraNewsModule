package org.cybotgalactica.pandoratracker;

import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.server.Server;
import org.javacord.api.event.message.MessageCreateEvent;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class DiscordBot {

    private static final String bindingsFile = ".bindings";
    private static final long testChannelId = 845753289793339472L;
    private static final long debugChannelId = 845772133660753920L;

    private final boolean isTestMode;
    private final TextChannel debugChannel;

    private final Map<Long, TextChannel> bindings = new HashMap<>();
    private final PandoraTracker tracker;
    private final DiscordApi api;

    public DiscordBot(String token, PandoraTracker tracker) {
        this(token, tracker, false);
    }

    public DiscordBot(String token, PandoraTracker tracker, boolean isTestMode) {
        this.tracker = tracker;
        api = new DiscordApiBuilder()
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
        this.isTestMode = isTestMode;

        this.debugChannel = api.getTextChannelById(debugChannelId).orElse(null);
        if (this.debugChannel == null) {
            System.out.printf("Could not find debug channel %d\n", debugChannelId);
        }
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
                    return String.format("Already bound to channel <#%d>", channel.getId());
                }
                bindings.put(channel.getId(), channel);
                return String.format("Bound PandoraNewsBot to channel <#%d>", channel.getId());
            } else {
                return "Can only bind to named channel if in a server";
            }
        } else {
            TextChannel channel = event.getChannel();
            Optional<ServerTextChannel> optionalServerTextChannel = channel.asServerTextChannel();
            if (bindings.containsKey(channel.getId())) {
                if (optionalServerTextChannel.isPresent()) {
                    return String.format("Already bound to channel <#%d>", optionalServerTextChannel.get().getId());
                } else {
                    return "Already bound to current channel";
                }
            }

            bindings.put(channel.getId(), channel);
            if (optionalServerTextChannel.isPresent()) {
                return String.format("Bound PandoraNewsBot to channel <#%d>", optionalServerTextChannel.get().getId());
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
                        return String.format("Could not find channel named '%s'", args[1]);
                    }
                }
                ServerTextChannel channel = channels.get(0);
                if (bindings.containsKey(channel.getId())) {
                    bindings.remove(channel.getId());
                    return String.format("Unbound PandoraNewsBot from channel <#%d>", channel.getId());
                } else {
                    return String.format("PandoraNewsBot is not bound to channel <#%d>", channel.getId());
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
                    return String.format("Unbound PandoraNewsBot from channel <#%d>", optionalServerTextChannel.get().getId());
                } else {
                    return "Unbound PandoraNewsBot from current channel";
                }
            } else {
                if (optionalServerTextChannel.isPresent()) {
                    return String.format("PandoraNewsBot is not bound to channel <#%d>", optionalServerTextChannel.get().getId());
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
                .map(c -> c.asServerTextChannel().map(sc -> String.format("<#%d>", sc.getId())).orElse("this channel"))
                .collect(Collectors.joining(", "));
    }

    public void sendUpdate(String string) {
        bindings.values().forEach(c -> c.sendMessage(string));
    }

    public void sendDebug(String string) {
        if (debugChannel != null) {
            debugChannel.sendMessage(string);
        }
    }

    public void preLoad() {
        if (isTestMode) {
            Optional<TextChannel> channel = api.getTextChannelById(testChannelId);
            if (channel.isPresent()) {
                bindings.put(testChannelId, channel.get());
            } else {
                System.out.printf("Could not find test channel with id %d%n\n", testChannelId);
            }
            return;
        }

        try (Scanner scanner = new Scanner(new File(bindingsFile))){
            while (scanner.hasNextLine()) {
                String data = scanner.nextLine();
                try {
                    long cId = Long.parseLong(data);
                    Optional<TextChannel> c = api.getTextChannelById(cId);
                    if (c.isPresent()) {
                        bindings.put(cId, c.get());
                    } else {
                        tracker.debug(String.format("Could not find channel with id %d, skipping", cId));
                    }
                } catch (NumberFormatException e) {
                    tracker.debug(String.format("Could not parse long id line %s", data));
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            tracker.debug(String.format("Could not load bindings file %s. Initializing with empty list", bindingsFile));

        }
    }

    public void postUnload() {
        if (isTestMode) {
            return;
        }
        System.out.println("Unload");
        try (FileWriter fileWriter = new FileWriter(bindingsFile)){
            try {
                for (long id : bindings.keySet()) {
                    fileWriter.write(Long.toString(id));
                    fileWriter.write('\n');
                }
            } catch (IOException e) {
                e.printStackTrace();
                String debugMessage = String.format("Error occured while writing to bindings file %s. Dumping channel id's:\n%s",
                        bindingsFile,
                        bindings.keySet().stream()
                                .map(id -> Long.toString(id))
                                .collect(Collectors.joining(", ")));
                System.out.printf("[debug] %s\n", debugMessage);
                tracker.debug(debugMessage);
            }
        } catch (IOException e) {
            e.printStackTrace();
            String debugMessage = String.format("Could not open bindings file %s for write. Dumping channel id's:\n%s",
                    bindingsFile,
                    bindings.keySet().stream()
                            .map(id -> Long.toString(id))
                            .collect(Collectors.joining(", ")));
            System.out.printf("[debug] %s\n", debugMessage);
            tracker.debug(debugMessage);
            //noinspection UnnecessaryReturnStatement
            return;
        }

    }
}
