package net.fakepixel.housing.world;

import net.fakepixel.housing.HousingPlugin;
import org.bukkit.Bukkit;

public final class WorldServiceFactory {
    private WorldServiceFactory() {}

    public static WorldService create(HousingPlugin plugin) {
        boolean useSwm = plugin.getStaticConfig().getBoolean("slimeworldmanager.enabled", true);
        if (useSwm && (Bukkit.getPluginManager().getPlugin("SlimeWorldManager") != null
                || Bukkit.getPluginManager().getPlugin("SlimeWorldPlugin") != null
                || Bukkit.getPluginManager().getPlugin("AdvancedSlimePaper") != null
                || hasClass())) {
            return new SlimeWorldManagerService(plugin);
        }
        return new BukkitTemplateWorldService(plugin);
    }

    private static boolean hasClass() {
        try {
            Class.forName("com.infernalsuite.asp.api.AdvancedSlimePaperAPI");
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }
}
