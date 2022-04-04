package org.cybotgalactica.pandoratracker;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.*;

public class TelegramBot extends TelegramLongPollingBot {

    private final PandoraTracker pandoraTracker;
    private final String token;

    public TelegramBot(String token) {
        this.token = token;
        pandoraTracker = new PandoraTracker();
    }

    public TelegramBot(PandoraTracker pandoraTracker, String token) {
        this.token = token;
        this.pandoraTracker = pandoraTracker;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (pandoraTracker == null) {
            System.out.println("Got update, but not initialized yet");
            return;
        }
        if (!update.hasMessage()) {
            return;
        }
        Message message = update.getMessage();
        if (!message.hasText() || !(message.getFrom().getUserName().equals("simon_struck") || message.getFrom().getUserName().equals("NielsOverkamp"))) {
            return;
        }
        String[] args = message.getText().split("\\s", 2);
        if (args.length == 0) {
            return;
        }
        String command = args[0];
        if (command.contains("@")) {
            command = command.substring(1, command.indexOf("@"));
        }
        switch (command) {
//            case "begin":
//                pandoraTracker.debug(Utils.parseUserName(update.getMessage().getFrom()) + " manually started the bot!");
//                pandoraTracker.linkTelegramBot(sender);
//                System.out.println("Started!");
//                break;
//            case "end":
//                pandoraTracker.debug(Utils.parseUserName(update.getMessage().getFrom()) + " manually stopped the bot!");
//                pandoraTracker.unlinkTelegramBot();
//                System.out.println("Stopped!");
//                break;
            case "channel":
                pandoraTracker.toggleOfficial();
                System.out.println("Toggled channels!");
                break;
//            case "scoreboard":
//                pandoraTracker.postScoreboard();
//                System.out.println("Printing Scoreboard!");
//                break;
            case "grouping":
                pandoraTracker.toggleGrouping();
                System.out.println("Toggled grouping!");
                break;
            case "say":
                if (args.length == 2) {
                    pandoraTracker.sendUpdate(args[1]);
                }
                break;
            default:
                System.out.println(update);
                break;
        }
    }

    public PandoraTracker getTracker() {
        return pandoraTracker;
    }

    @Override
    public String getBotUsername() {
        return "PandoraNewsBot";
    }

    @Override
    public String getBotToken() {
        return token;
    }
}
