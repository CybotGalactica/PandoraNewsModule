package org.simonscode.telegrambots.framework.modules.pandoratracker;


import com.google.auto.service.AutoService;
import org.simonscode.telegrambots.framework.*;
import org.telegram.telegrambots.api.objects.Message;
import org.telegram.telegrambots.api.objects.Update;

@AutoService(Module.class)
public class PandoraTrackerModule extends ModuleAdapter {

    //    private final long commandSource = -1001210020895L;
    private State state = new State();
    private PandoraTracker pandoraTracker = new PandoraTracker();

    @Override
    public ModuleInfo getModuleInfo() {
        return new ModuleInfo("PandoraTracker", "2.0-SNAPSHOT", "Simon Struck", ModuleInfo.InstanciationPereference.SINGLE_INSTANCE_ACROSS_ALL_BOTS);
    }

    @Override
    public void processUpdate(Bot sender, Update update) {
        if (!update.hasMessage()) {
            return;
        }
        Message message = update.getMessage();
        if (!message.hasText() || !message.getFrom().getUserName().equals("simon_struck")) {
            return;
        }
        String command = message.getText();
        if (command.length() < 2) {
            return;
        }
        if (command.contains("@")) {
            command = command.substring(1, command.indexOf("@"));
        }
        switch (command) {
            case "begin":
                pandoraTracker.debug(Utils.parseUserName(update.getMessage().getFrom()) + " manually started the bot!");
                pandoraTracker.start(sender);
                System.out.println("Started!");
                break;
            case "end":
                pandoraTracker.debug(Utils.parseUserName(update.getMessage().getFrom()) + " manually stopped the bot!");
                pandoraTracker.stop();
                System.out.println("Stopped!");
                break;
            case "channel":
                pandoraTracker.toggleOfficial();
                System.out.println("Toggled channels!");
                break;
            case "scoreboard":
                pandoraTracker.postScoreboard();
                System.out.println("Printing Scoreboard!");
                break;
            case "grouping":
                pandoraTracker.toggleGrouping();
                System.out.println("Toggled grouping!");
                break;
            default:
                Utils.logUpdate(update);
                break;
        }
    }

    @Override
    public void initialize(org.simonscode.telegrambots.framework.State state) {
        if (state != null) {
            this.state = (State) state;
        }
    }

    @Override
    public void postLoad(Bot bot) {
        pandoraTracker.start(bot);
    }

    @Override
    public void preUnload(Bot bot) {
        pandoraTracker.stop();
    }


    @Override
    public org.simonscode.telegrambots.framework.State saveState(Bot bot) {
        return state;
    }

    @Override
    public Class<? extends org.simonscode.telegrambots.framework.State> getStateType() {
        return State.class;
    }

    public PandoraTracker getTracker() {
        return pandoraTracker;
    }
}
