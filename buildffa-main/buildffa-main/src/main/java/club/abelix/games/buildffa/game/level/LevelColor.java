package club.abelix.games.buildffa.game.level;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.val;
import org.bukkit.ChatColor;

@Getter
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public enum LevelColor {
    GRAY(0, 99, ChatColor.GRAY),
    WHITE(100, 199, ChatColor.WHITE),
    GOLD(200, 299, ChatColor.GOLD),
    AQUA(300, 399, ChatColor.AQUA),
    DARK_GREEN(400, 499, ChatColor.DARK_GREEN),
    DARK_AQUA(500, 599, ChatColor.DARK_AQUA),
    DARK_RED(600, 699, ChatColor.DARK_RED),
    LIGHT_PURPLE(700, 799, ChatColor.LIGHT_PURPLE),
    BLUE(800, 899, ChatColor.BLUE),
    DARK_PURPLE(900, 999, ChatColor.DARK_PURPLE);

    int minLevel;
    int maxLevel;
    ChatColor color;

    public static LevelColor getByLevel(int level) {
        for (val color : LevelColor.values())
            if (level >= color.minLevel && level <= color.maxLevel)
                return color;

        return GRAY;
    }
}
