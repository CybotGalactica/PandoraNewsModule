package org.simonscode.telegrambots.framework.modules.pandoratracker;


import com.google.auto.service.AutoService;
import org.simonscode.telegrambots.framework.*;
import org.telegram.telegrambots.api.objects.Message;
import org.telegram.telegrambots.api.objects.Update;

@AutoService(Module.class)
public class PandoraTrackerModule extends ModuleAdapter {

    private State state = new State();
    private PandoraTracker pandoraTracker = new PandoraTracker();

    @Override
    public ModuleInfo getModuleInfo() {
        return new ModuleInfo("PandoraTracker", "2.0-SNAPSHOT", "Simon Struck", ModuleInfo.InstanciationPereference.SINGLE_INSTANCE_ACROSS_ALL_BOTS);
    }

    @Override
    public void processUpdate(Bot sender, Update update) {
        Message message = Utils.checkForCommand(update, "/stop");
        if (message != null && message.getFrom().getFirstName().startsWith("Simon")) {
            pandoraTracker.sendDebug(Utils.parseUserName(update.getMessage().getFrom()) + " manually stopped the bot!");
            pandoraTracker.stop();
            return;
        }

        message = Utils.checkForCommand(update, "/start");
        if (message != null && message.getFrom().getFirstName().startsWith("Simon")) {
            pandoraTracker.sendDebug(Utils.parseUserName(update.getMessage().getFrom()) + " manually started the bot!");
            pandoraTracker.start(sender);
            return;
        }

        message = Utils.checkForCommand(update, "/toogle");
        if (message != null && message.getFrom().getFirstName().startsWith("Simon")) {
            pandoraTracker.toggleOfficial();
            return;
        }

        System.out.println(update);
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

}
