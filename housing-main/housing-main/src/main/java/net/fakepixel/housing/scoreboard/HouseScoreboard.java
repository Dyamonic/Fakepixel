package net.fakepixel.housing.scoreboard;

import net.fakepixel.housing.house.House;
import org.bukkit.entity.Player;
import java.util.ArrayList;

public final class HouseScoreboard implements ScoreboardProvider {
    public ScoreboardView create(Player player,House house){return new ScoreboardView(house==null?"&b&lHOUSING":house.getScoreboardTitle(),house==null?new ArrayList<String>():new ArrayList<>(house.getScoreboardLines()));}
}
