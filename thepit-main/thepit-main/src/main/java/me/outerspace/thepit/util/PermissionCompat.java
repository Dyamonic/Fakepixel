package me.outerspace.thepit.util;

import me.outerspace.thepit.ThePitPlugin;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PermissionCompat {
    private static final Map<String, Long> warnings = new ConcurrentHashMap<>();

    private PermissionCompat() {}

    public static boolean has(ThePitPlugin plugin, CommandSender sender, String permission) {
        if (sender == null || permission == null || permission.isEmpty()) return false;
        String modern = permission.startsWith("buildffa.") ? "thepit." + permission.substring(9) : permission;
        if (sender.hasPermission(modern)) return true;
        if (!modern.startsWith("thepit.")) return false;
        String legacy = "buildffa." + modern.substring(7);
        if (!sender.hasPermission(legacy)) return false;
        if (legacy.startsWith("buildffa.admin") && plugin != null) warn(plugin, sender, legacy, modern);
        return true;
    }

    private static void warn(ThePitPlugin plugin, CommandSender sender, String legacy, String modern) {
        String identity = sender instanceof Player ? ((Player) sender).getUniqueId().toString() : "console";
        String key = identity + ':' + legacy;
        long now = System.currentTimeMillis();
        Long previous = warnings.put(key, now);
        if (previous == null || now - previous >= 600000L) {
            plugin.getLogger().warning("Legacy permission " + legacy + " was used by " + sender.getName() + "; grant " + modern + " instead.");
        }
    }
}
