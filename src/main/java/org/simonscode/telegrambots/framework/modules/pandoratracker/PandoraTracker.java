package org.simonscode.telegrambots.framework.modules.pandoratracker;

import org.simonscode.telegrambots.framework.Bot;
import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.TelegramBotsApi;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.exceptions.TelegramApiException;
import org.telegram.telegrambots.exceptions.TelegramApiRequestException;

import java.util.Collections;

public class PandoraTracker {

    // Private
    private final long debugChannel = -1001334509240L;
    private final long targetChannel = -1001334509240L;
    // Official Channel
    private final String officialChannel = "@pandonews";

    // Group Chat
    //        private final long debugChannel = -1001210020895L;

    public static void main(String[] args) throws TelegramApiRequestException {

        ApiContextInitializer.init();
        TelegramBotsApi api = new TelegramBotsApi();

        PandoraTracker pandoraTracker = new PandoraTracker();
        Bot bot = new Bot("Bot", args[0], Collections.singletonList(new PandoraTrackerModule()));
        api.registerBot(bot);
        pandoraTracker.start(bot);
    }

    private Bot bot;
    private Database db;
    private WSClient wsKillFeed;
    private WSClient wsKillShout;
    private WSClient wsPuzzleFeed;
    private WSClient wsNewsFeed;

    public PandoraTracker() {

    }

    public void start(Bot bot) {
        this.bot = bot;
        db = new Database(this);
        wsKillFeed = new WSClient(this, Message.Type.KILLFEED, "wss://iapandora.nl/ws/killfeed?subscribe-broadcast", this::onMessage);
        wsKillShout = new WSClient(this, Message.Type.KILLSHOUT, "wss://iapandora.nl/ws/killshout?subscribe-broadcast", this::onMessage);
        wsPuzzleFeed = new WSClient(this, Message.Type.PUZZLE, "wss://iapandora.nl/ws/puzzlefeed?subscribe-broadcast", this::onMessage);
        wsNewsFeed = new WSClient(this, Message.Type.NEWS, "wss://iapandora.nl/ws/news?subscribe-broadcast", this::onMessage);

        sendDebug("Bot is up and running!");
    }

    public void stop() {
        wsKillFeed.close();
        wsKillShout.close();
        wsPuzzleFeed.close();
        wsNewsFeed.close();
        db.close();
    }

    private void onMessage(Message.Type type, String update) {
        if (update.equals("--heartbeat--")) {
            return;
        }
        System.out.println("Received: " + update);
        db.insertMessage(update);
        Message message = new Message(type, update);
        sendUpdate(message);
    }

    private void sendUpdate(Message updateMessage) {
        try {
            SendMessage sendMessage = updateMessage.getSendMessage();
            sendMessage.setChatId(targetChannel);
            bot.execute(sendMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public void sendDebug(String debugMessage) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(debugChannel);
        sendMessage.setText("Debug: " + debugMessage);
        try {
            bot.execute(sendMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}
