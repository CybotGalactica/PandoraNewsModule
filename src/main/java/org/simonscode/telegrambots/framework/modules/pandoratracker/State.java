package org.simonscode.telegrambots.framework.modules.pandoratracker;

import java.util.ArrayList;
import java.util.List;

public class State extends org.simonscode.telegrambots.framework.State {

    private List<News> news = new ArrayList<>();
    private List<Team> treams = new ArrayList<>();

    public List<News> getNews() {
        return news;
    }

    public void setNews(List<News> news) {
        this.news = news;
    }

    public List<Team> getTreams() {
        return treams;
    }

    public void setTreams(List<Team> treams) {
        this.treams = treams;
    }
}
