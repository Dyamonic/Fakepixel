package club.abelix.api.base.util.chat;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.DyeColor;

/**
 * <h3>ColorUtil by suuft (Shushpanov Valeriy)</h3>
 * Writed at 15/05/2023 15:29
 * <br><br> NOTE: Other solution - a map, but it seems to me that it is better.
 */
@Getter
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public enum ColorType {

    BLACK(ChatColor.BLACK, DyeColor.BLACK, 0),
    DARK_BLUE(ChatColor.DARK_BLUE, DyeColor.BLUE, 4),
    DARK_GREEN(ChatColor.DARK_GREEN, DyeColor.GREEN, 2),
    DARK_AQUA(ChatColor.DARK_AQUA, DyeColor.CYAN, 6),
    RED(ChatColor.RED, DyeColor.RED, 1),
    DARK_PURPLE(ChatColor.DARK_PURPLE, DyeColor.PURPLE, 5),
    DARK_RED(ChatColor.DARK_RED, DyeColor.RED, 1),
    GOLD(ChatColor.GOLD, DyeColor.ORANGE, 14),
    DARK_GRAY(ChatColor.DARK_GRAY, DyeColor.GRAY, 8),
    BLUE(ChatColor.BLUE, DyeColor.LIGHT_BLUE, 12),
    GREEN(ChatColor.GREEN, DyeColor.LIME, 10),
    LIGHT_PURPLE(ChatColor.LIGHT_PURPLE, DyeColor.MAGENTA, 9),
    YELLOW(ChatColor.YELLOW, DyeColor.YELLOW, 11),
    WHITE(ChatColor.WHITE, DyeColor.WHITE, 15);

    ChatColor chatColor;
    DyeColor dyeColor;
    int dyeDurability;

    /**
     * Get ColorType from ChatColor.
     */
    public static ColorType getColorType(ChatColor chatColor) {
        for (ColorType entry : values()) {
            if (entry.chatColor.equals(chatColor)) {
                return entry;
            }
        }
        return RED;
    }

    /**
     * Get ColorType from abelix ChatColor.
     */
    public static ColorType getColorType(club.abelix.commons.util.color.ChatColor chatColor) {
        for (ColorType entry : values()) {
            if (entry.chatColor.toString().equals(chatColor.toString())) {
                return entry;
            }
        }
        return RED;
    }

    /**
     * Get ColorType from DyeColor.
     */
    public static ColorType getColorType(@NonNull DyeColor dyeColor) {
        for (ColorType entry : values()) {
            if (entry.dyeColor.equals(dyeColor)) {
                return entry;
            }
        }
        return RED;
    }

    public Color getColor() {
        return dyeColor.getColor();
    }
}
