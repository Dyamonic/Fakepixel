package club.abelix.games.fakevival.config;

import club.abelix.games.fakevival.Game;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class RuntimeSettings {
    private RuntimeSettings() {
    }

    public static FileConfiguration config(JavaPlugin plugin) {
        if (plugin instanceof Game game) {
            return game.settings();
        }
        return plugin.getConfig();
    }
}
