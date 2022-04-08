package org.cybotgalactica.pandoratracker;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.cybotgalactica.pandoratracker.models.Message;
import org.cybotgalactica.pandoratracker.models.PandoraUpdate;

import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

public class PandoraTracker implements CommandConsumer {
    private static final long GROUP_MESSAGE_INTERVAL = 10_000;

    private final ConcurrentLinkedQueue<PandoraUpdate> messageQueue = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<String> debugQueue = new ConcurrentLinkedQueue<>();

    private final Timer messageTimer = new Timer();

    private boolean grouping = true;
    private boolean debugGrouping = true;

    private final boolean isOfficial;

    private final List<MessageConsumer> messageConsumers = new ArrayList<>();
    private final List<MessageConsumer> debugMessageConsumers = new ArrayList<>();

    private Gson gson;

    public PandoraTracker(boolean isOfficial) {
        this.isOfficial = isOfficial;
    }

    public void start() {
        gson = new GsonBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .create();

        messageTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (grouping) {
                    sendMessageIfNeeded();
                }
            }
        }, GROUP_MESSAGE_INTERVAL, GROUP_MESSAGE_INTERVAL);

        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                sendDebug(String.format("%s-bot is up and running!", isOfficial ? "Production" : "Testing"));
            }
        }, 3_000);
    }

    public void addMessageConsumer(MessageConsumer messageConsumer) {
        messageConsumers.add(messageConsumer);
    }

    public void addDebugMessageConsumer(MessageConsumer messageConsumer) {
        debugMessageConsumers.add(messageConsumer);
    }

    private void sendMessageIfNeeded() {
        if (!messageQueue.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            while (!messageQueue.isEmpty()) {
                sb.append(Objects.requireNonNull(messageQueue.poll()).getText());
                if (!messageQueue.isEmpty()) {
                    sb.append('\n');
                }
            }
            sendMessage(sb.toString());
        }
        if (!debugQueue.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            while (!debugQueue.isEmpty()) {
                sb.append(String.format("[%s] %s", isOfficial ? "Production" : "Testing", debugQueue.poll()));
                if (!debugQueue.isEmpty()) {
                    sb.append('\n');
                }
            }
            sendDebug(sb.toString());
        }
    }
    
    public void sendMessage(String text) {
        if (messageConsumers.isEmpty()) {
            System.out.printf("Warning, the message \"%s\" did not get sent\n", text);
        } else {
            Message message = new Message(text);
            messageConsumers.forEach((consumer) -> consumer.consumeMessage(message));
        }
    }

    private void sendDebug(String text) {
        if (debugMessageConsumers.isEmpty()) {
            System.out.printf("Warning, the debug message \"%s\" did not get sent\n", text);
        } else {
            Message message = new Message(text);
            debugMessageConsumers.forEach((consumer) -> consumer.consumeMessage(message));
        }
    }

    public void queueDebug(String debugMessage) {
        debugQueue.add(debugMessage);
    }

    public void onUpdate(String updateMessage) {
        Type updateList = new TypeToken<ArrayList<PandoraUpdate>>() {}.getType();
        List<PandoraUpdate> pandoraUpdate = gson.fromJson(updateMessage, updateList);
        messageQueue.addAll(pandoraUpdate);
        System.out.printf("Received %d updates\n", pandoraUpdate.size());
        if (!grouping) {
            sendMessageIfNeeded();
        }
    }

    @Override
    public void close() {
        // TODO
        sendDebug("Shutting down PandoraNews...");
    }

    @Override
    public void toggleGrouping() {
        grouping = !grouping;
        queueDebug((grouping ? "Enabled" : "Disabled") + " grouping");
    }

    @Override
    public void toggleDebugGrouping() {
        debugGrouping = !debugGrouping;
        queueDebug((grouping ? "Enabled" : "Disabled") + " grouping");
    }

    @Override
    public void say(String text) {
        sendMessage(text);
    }
}
