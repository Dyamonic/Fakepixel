package net.fakepixel.housing.scoreboard;

import net.fakepixel.housing.house.House;
import org.bukkit.entity.Player;

public interface ScoreboardProvider {
    ScoreboardView create(Player player, House house);
}
