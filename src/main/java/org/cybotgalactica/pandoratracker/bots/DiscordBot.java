package org.cybotgalactica.pandoratracker.bots;

import org.cybotgalactica.pandoratracker.MessageConsumer;
import org.cybotgalactica.pandoratracker.models.Message;
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
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

public class DiscordBot {
    private static final String BINDINGS_FILE = "discord.bindings";
    private static final long TEST_CHANNEL_ID = 845753289793339472L;
    private static final long DEBUG_CHANNEL_ID = 845772133660753920L;

    private final boolean isTestMode;
    private final TextChannel debugChannel;

    private final Map<Long, TextChannel> bindings = new HashMap<>();
    private final DiscordApi api;

    private final MessageConsumer debugConsumer;
    private final Timer messageTimer = new Timer();
    private ConcurrentLinkedQueue<ChannelMessage> backlog = new ConcurrentLinkedQueue<>();

    public DiscordBot(String token, MessageConsumer debugConsumer) {
        this(token, debugConsumer, false);
    }

    public DiscordBot(String token, MessageConsumer debugConsumer, boolean isTestMode) {
        api = new DiscordApiBuilder()
                .setToken(token)
                .login().join();
        
        this.debugConsumer = debugConsumer;

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

        this.debugChannel = api.getTextChannelById(DEBUG_CHANNEL_ID).orElse(null);
        if (this.debugChannel == null) {
            System.out.printf("Could not find debug channel %d\n", DEBUG_CHANNEL_ID);
        }

        messageTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                sendBacklog();
            }
        }, 10_000, 10_000);
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
        for (TextChannel c : bindings.values()) {
            c.sendMessage(string)
                    .whenComplete((r, e) -> {
                        if (e != null && r == null) {
                            e.printStackTrace();
                            debugConsumer.consumeMessage(new org.cybotgalactica.pandoratracker.models.Message(
                                    String.format("Telegram bot failed to send message %s in channel %s",
                                            string,
                                            c)));
                            backlog.add(new ChannelMessage(string, c));
                        }
                    });
        }
    }

    public void sendDebug(String string) {
        if (debugChannel != null) {
            debugChannel.sendMessage(string)
                    .whenComplete((r, e) -> {
                        if (e != null && r == null) {
                            e.printStackTrace();
                        }
                    });
        }
    }

    private void sendBacklog() {
        while (!backlog.isEmpty()) {
            ChannelMessage channelMessage = backlog.poll();
            channelMessage.channel.sendMessage(channelMessage.message).whenComplete((r,e) -> {
                if (e != null && r == null) {
                    e.printStackTrace();
                    backlog.add(channelMessage);
                }
            });
        }
    }

    public void preLoad() {
        if (isTestMode) {
            Optional<TextChannel> channel = api.getTextChannelById(TEST_CHANNEL_ID);
            if (channel.isPresent()) {
                bindings.put(TEST_CHANNEL_ID, channel.get());
            } else {
                System.out.printf("Could not find test channel with id %d%n\n", TEST_CHANNEL_ID);
            }
            return;
        }

        try (Scanner scanner = new Scanner(new File(BINDINGS_FILE))){
            while (scanner.hasNextLine()) {
                String data = scanner.nextLine();
                try {
                    long cId = Long.parseLong(data);
                    Optional<TextChannel> c = api.getTextChannelById(cId);
                    if (c.isPresent()) {
                        bindings.put(cId, c.get());
                    } else {
                        debugConsumer.consumeMessage(new Message(String.format("Could not find channel with id %d, skipping", cId)));
                    }
                } catch (NumberFormatException e) {
                    debugConsumer.consumeMessage(new Message(String.format("Could not parse long id line %s", data)));
                }
            }
        } catch (FileNotFoundException e) {
            debugConsumer.consumeMessage(new Message(String.format("Could not load bindings file %s. Initializing with empty list", BINDINGS_FILE)));
        }
    }

    public void postUnload() {
        if (isTestMode) {
            return;
        }
        System.out.println("Unload");
        try (FileWriter fileWriter = new FileWriter(BINDINGS_FILE)){
            try {
                for (long id : bindings.keySet()) {
                    fileWriter.write(Long.toString(id));
                    fileWriter.write('\n');
                }
            } catch (IOException e) {
                e.printStackTrace();
                String debugMessage = String.format("Error occured while writing to bindings file %s. Dumping channel id's:\n%s",
                        BINDINGS_FILE,
                        bindings.keySet().stream()
                                .map(id -> Long.toString(id))
                                .collect(Collectors.joining(", ")));
                System.out.printf("[debug] %s\n", debugMessage);
                debugConsumer.consumeMessage(new Message(debugMessage));
            }
        } catch (IOException e) {
            e.printStackTrace();
            String debugMessage = String.format("Could not open bindings file %s for write. Dumping channel id's:\n%s",
                    BINDINGS_FILE,
                    bindings.keySet().stream()
                            .map(id -> Long.toString(id))
                            .collect(Collectors.joining(", ")));
            System.out.printf("[debug] %s\n", debugMessage);
            debugConsumer.consumeMessage(new Message(debugMessage));
            //noinspection UnnecessaryReturnStatement
            return;
        }

    }

    private static class ChannelMessage {
        String message;
        TextChannel channel;

        public ChannelMessage(String message, TextChannel channel) {
            this.message = message;
            this.channel = channel;
        }
    }
}
