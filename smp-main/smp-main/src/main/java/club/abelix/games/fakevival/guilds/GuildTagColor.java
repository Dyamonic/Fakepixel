package club.abelix.games.fakevival.guilds;

import org.bukkit.Material;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

public enum GuildTagColor {
    GRAY("&7", Material.GRAY_DYE),
    WHITE("&f", Material.WHITE_DYE),
    GREEN("&a", Material.LIME_DYE),
    AQUA("&b", Material.LIGHT_BLUE_DYE),
    RED("&c", Material.RED_DYE),
    LIGHT_PURPLE("&d", Material.PINK_DYE),
    YELLOW("&e", Material.YELLOW_DYE),
    GOLD("&6", Material.ORANGE_DYE),
    DARK_GREEN("&2", Material.GREEN_DYE),
    DARK_AQUA("&3", Material.CYAN_DYE),
    DARK_RED("&4", Material.RED_STAINED_GLASS),
    DARK_PURPLE("&5", Material.PURPLE_DYE),
    BLUE("&9", Material.BLUE_DYE),
    DARK_BLUE("&1", Material.BLUE_STAINED_GLASS),
    BLACK("&0", Material.BLACK_DYE);

    private final String code;
    private final Material icon;

    GuildTagColor(String code, Material icon) {
        this.code = code;
        this.icon = icon;
    }

    public String code() {
        return code;
    }

    public Material icon() {
        return icon;
    }

    public static GuildTagColor safe(String value) {
        return parse(value).orElse(GRAY);
    }

    public static Optional<GuildTagColor> parse(String value) {
        if (value == null || value.isBlank()) return Optional.empty();
        String normalized = value.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        return Arrays.stream(values())
                .filter(color -> color.name().equals(normalized) || color.code.equalsIgnoreCase(value.trim()))
                .findFirst();
    }
}
