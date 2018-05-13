package org.simonscode.telegrambots.framework.modules.pandoratracker;

public class News {
    private String message;
    private NewsType type;
    private String srcTeam;
    private String dstTeam;
    private String srcUser;
    private String dstUser;

    public News(String message) {
        this.message = message;
        type = NewsType.SOLVE;
    }

    public News(String message, String srcTeam, String dstTeam, String srcUser, String dstUser) {
        this.message = message;
        this.srcTeam = srcTeam.replace("amp;", "");
        this.dstTeam = dstTeam.replace("amp;", "");
        this.srcUser = srcUser;
        this.dstUser = dstUser;
        type = NewsType.KILL;
    }

    public String getSrcUser() {
        return srcUser;
    }

    public void setSrcUser(String srcUser) {
        this.srcUser = srcUser;
    }

    public String getDstUser() {
        return dstUser;
    }

    public void setDstUser(String dstUser) {
        this.dstUser = dstUser;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public NewsType getType() {
        return type;
    }

    public void setType(NewsType type) {
        this.type = type;
    }

    public String getSrcTeam() {
        return srcTeam;
    }

    public void setSrcTeam(String srcTeam) {
        this.srcTeam = srcTeam;
    }

    public String getDstTeam() {

        return dstTeam;
    }

    public void setDstTeam(String dstTeam) {
        this.dstTeam = dstTeam;
    }

    @Override
    public String toString() {
        return message;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        News news = (News) o;

        return message.equals(news.message);
    }

    @Override
    public int hashCode() {
        return message.hashCode();
    }
}
