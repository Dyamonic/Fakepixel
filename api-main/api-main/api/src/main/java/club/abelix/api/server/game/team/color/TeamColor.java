package club.abelix.api.server.game.team.color;

import club.abelix.commons.language.Language;
import lombok.Getter;
import lombok.NonNull;
import org.bukkit.ChatColor;
import org.bukkit.Color;

import java.util.Arrays;

public enum TeamColor {
    RED(ChatColor.RED, Color.RED, (short) 14),
    BLUE(ChatColor.BLUE, Color.BLUE, (short) 11),

    GREEN(ChatColor.GREEN, Color.LIME, (short) 5),
    YELLOW(ChatColor.YELLOW, Color.YELLOW, (short) 4),

    GOLD(ChatColor.GOLD, Color.ORANGE, (short) 1),
    PINK(ChatColor.LIGHT_PURPLE, Color.FUCHSIA, (short) 2),

    DARK_AQUA(ChatColor.AQUA, Color.AQUA, (short) 9),
    GRAY(ChatColor.DARK_GRAY, Color.GRAY, (short) 7),
    WHITE(ChatColor.WHITE, Color.WHITE, (short) 0);

    // -------------------------------------------
    public static final TeamColor[] VALUES = values();
    // -------------------------------------------

    @Getter
    private final ChatColor color;
    private final Color bukkitColor;
    private final short data;

    TeamColor(ChatColor color, Color bukkitColor, short data) {
        this.color = color;
        this.bukkitColor = bukkitColor;
        this.data = data;
    }

    public static TeamColor[] getTypes(int num) {
        return Arrays.copyOf(TeamColor.values(), num);
    }

    public String getColorCode() {
        return color.toString();
    }

    // their - их  - их сломали
    public String getNameTheir(@NonNull Language language) {
        return color.toString() + language.getMessage("team_color_" + name() + "_their");
    }

    // them - им  - им написали в чат
    public String getNameThem(@NonNull Language language) {
        return color.toString() + language.getMessage("team_color_" + name() + "_them");
    }

    // they  - они - они победили
    public String getNameThey(@NonNull Language language) {
        return color.toString() + language.getMessage("team_color_" + name() + "_they");
    }

    // he    - он  - он выпал из мира
    public String getNameHe(@NonNull Language language) {
        return color.toString() + language.getMessage("team_color_" + name() + "_he");
    }

    public Color toBukkitColor() {
        return bukkitColor;
    }

    public short getColorData() {
        return data;
    }

}
