package club.abelix.games.buildffa.game.level;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.commons.data.level.Leveling;
import club.abelix.commons.data.stat.StatsManager;
import club.abelix.commons.data.stat.StatsMode;
import club.abelix.network.type.mode.Master;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import lombok.val;
import org.bukkit.ChatColor;

@UtilityClass
public class LevelingUtil {
    private final ChatColor[] RAINBOW_COLORS = new ChatColor[]{
            ChatColor.RED,
            ChatColor.GOLD,
            ChatColor.YELLOW,
            ChatColor.GREEN,
            ChatColor.AQUA,
            ChatColor.LIGHT_PURPLE,
            ChatColor.DARK_PURPLE,
    };

    private final StatsMode STATS_MODE = StatsManager.INSTANCE.getMode(Master.BW);

    public String getFormattedLevel(@NonNull Gamer gamer, boolean simple) {
        int level = getLevel(gamer);

        val color = LevelColor.getByLevel(level).getColor();
        return String.format(simple ? "%s%d " : "%s[%d] ", color, level);
    }

    public int getLevel(@NonNull Gamer gamer) {
        return Leveling.getLevel(getExperience(gamer));
    }

    public int getExperience(@NonNull Gamer gamer) {
        return gamer.getLifetimeStat(STATS_MODE, "experience");
    }

    public String rainbowText(String text) {
        val builder = new StringBuilder();
        int index = 0;

        for (int i = 0; i < text.length(); i++) {
            builder.append(RAINBOW_COLORS[index]);
            builder.append(text.charAt(i));

            index = (index + 1) % RAINBOW_COLORS.length;
        }

        return builder.toString();
    }
}
