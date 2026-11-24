package club.abelix.games.fakevival.util;

import org.bukkit.ChatColor;

import java.util.ArrayList;
import java.util.List;

public final class CC {
    private CC() {
    }

    public static String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text == null ? "" : text);
    }

    public static List<String> colorList(List<String> lines) {
        List<String> colored = new ArrayList<>();
        for (String line : lines) colored.add(color(line));
        return colored;
    }
}
