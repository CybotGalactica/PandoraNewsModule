package org.simonscode.telegrambots.framework.modules.pandoratracker;


import com.google.auto.service.AutoService;
import org.simonscode.telegrambots.framework.Bot;
import org.simonscode.telegrambots.framework.Module;
import org.simonscode.telegrambots.framework.ModuleInfo;
import org.simonscode.telegrambots.framework.Utils;
import org.telegram.telegrambots.api.objects.Message;
import org.telegram.telegrambots.api.objects.Update;

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Created by simon on 16.05.17.
 */
@AutoService(Module.class)
public class PandoraTrackerModule implements Module {
    private static final long CHATID = -1001050885996L;
    private PreparedStatement insertMessage;
    private long targetChannel = 282400797L;
    //    private String targetChannel = "@pandonews";

    private State state = new State();
    private boolean isRunning = false;

    @Override
    public void preLoad(Bot bot) {
        try {
            Connection connection = DriverManager.getConnection("jdbc:sqlite:database.sqlite");
            connection.setAutoCommit(true);
            insertMessage = connection.prepareStatement("INSERT INTO messages (currentTime, message) VALUES (current_time(), ?);");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    @Override
    public ModuleInfo getModuleInfo() {
        return new ModuleInfo("PandoraTracker", "2.0-SNAPSHOT", "Simon Struck", ModuleInfo.InstanciationPereference.SINGLE_INSTANCE_ACROSS_ALL_BOTS);
    }

    @Override
    public void initialize(org.simonscode.telegrambots.framework.State state) {
        if (state != null) {
            this.state = (State) state;
        }
    }

    @Override
    public void postLoad(Bot bot) {
        isRunning = true;
        try {
            WebsocketClient websocketClient = new WebsocketClient(new URI("wss://iapandora.nl/ws/killfeed?subscribe-broadcast")) {
                @Override
                public void onMessage(String message) {

                }

                @Override
                public void onClose(int code, String reason, boolean remote) {

                }
            };
            websocketClient.connect();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void processUpdate(Bot sender, Update update) {
        Message message = Utils.checkForCommand(update, "/done");
        if (message != null && message.getFrom().getFirstName().startsWith("Simon")) {
            System.out.println("Done!");
        }
        System.out.println(update);
    }

    @Override
    public void preUnload(Bot bot) {
        isRunning = false;
    }

    @Override
    public void postUnload(Bot bot) {

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
