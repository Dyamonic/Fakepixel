package club.abelix.games.fastbridge.commons.utils;

import club.abelix.games.fastbridge.commons.config.IslandConfig;
import lombok.experimental.UtilityClass;
import lombok.val;
import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.World;

@UtilityClass
public class WorldUtility {

    public void prepareWorld(final World world) {
        world.setTime(0);
        world.setWeatherDuration(0);

        world.setStorm(false);
        world.setThundering(false);
        world.setDifficulty(Difficulty.PEACEFUL);

        world.setGameRuleValue("doDaylightCycle", "false");
        world.setGameRuleValue("doWeatherCycle", "false");
    }

    public boolean canJoinWorld(final IslandConfig config) {
        val worldName = config.worldName();
        val maxPlayers = config.islandsCount() - 1;
        val world = Bukkit.getWorld(worldName);

        return world != null && world.getPlayers().size() < maxPlayers;
    }

}
