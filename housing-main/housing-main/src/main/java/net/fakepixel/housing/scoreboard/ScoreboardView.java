package net.fakepixel.housing.scoreboard;

import java.util.List;

public final class ScoreboardView {
    private final String title;
    private final List<String> lines;
    public ScoreboardView(String title,List<String> lines){this.title=title;this.lines=lines;}
    public String getTitle(){return title;} public List<String> getLines(){return lines;}
}
