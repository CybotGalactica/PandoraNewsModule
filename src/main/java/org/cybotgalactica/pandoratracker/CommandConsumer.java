package org.cybotgalactica.pandoratracker;

public interface CommandConsumer {
    void close();
    void toggleGrouping();
    void toggleDebugGrouping();
    void say(String text);
}
