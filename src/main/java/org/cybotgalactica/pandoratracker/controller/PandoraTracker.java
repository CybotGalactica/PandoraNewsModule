package org.cybotgalactica.pandoratracker.controller;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import lombok.Getter;
import lombok.Setter;
import org.cybotgalactica.pandoratracker.models.Update;
import org.cybotgalactica.pandoratracker.bots.TrackerBot;
import org.cybotgalactica.pandoratracker.models.BotMessage;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Component
public class PandoraTracker {

    @Getter
    @Setter
    private AtomicBoolean isPaused = new AtomicBoolean(true); // TODO Configurable? Reconsider?

    private final ConcurrentLinkedQueue<Update> messageQueue = new ConcurrentLinkedQueue<>();

    @SuppressWarnings({"FieldCanBeLocal", "FieldMayBeFinal"})
    private long timeBetweenMessages = 10_000;
    private final Gson gson = new GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .create();

    @Getter
    private final BotRegistry botRegistry = new BotRegistry(this);

    public PandoraTracker() {
        new Timer().scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (!isPaused.get()) {
                    sendMessagesIfNeeded();
                }
            }
        }, timeBetweenMessages, timeBetweenMessages);
    }

    public void sendMessagesIfNeeded() {
        if (!messageQueue.isEmpty()) {
            BotMessage message = new BotMessage(
                    messageQueue.stream().map(Update::getText).collect(Collectors.toList())
            );
            botRegistry.sendMessage(message);
        }
    }

    public void onUpdate(String updateMessage) {
        Type updateList = new TypeToken<ArrayList<Update>>() {
        }.getType();
        List<Update> update = gson.fromJson(updateMessage, updateList);
        messageQueue.addAll(update);
        System.out.printf("Received %d updates\n", update.size());
    }
}
