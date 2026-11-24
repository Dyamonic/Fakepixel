package club.abelix.games.fakevival.managers;

import club.abelix.games.fakevival.util.CC;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class SocialSpyManager {
    private final JavaPlugin plugin;
    private final Set<UUID> enabled = new HashSet<>();
    private final Set<UUID> disabled = new HashSet<>();

    public SocialSpyManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean toggle(Player player) {
        UUID uuid = player.getUniqueId();
        if (isEnabled(player)) {
            enabled.remove(uuid);
            disabled.add(uuid);
            return false;
        }
        disabled.remove(uuid);
        enabled.add(uuid);
        return true;
    }

    public boolean isEnabled(Player player) {
        UUID uuid = player.getUniqueId();
        boolean autoEnabled = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getBoolean("socialspy.auto-enabled", false)
                && player.hasPermission("fakevival.staff.socialspy.auto");
        return !disabled.contains(uuid) && (enabled.contains(uuid) || autoEnabled);
    }

    public void remove(Player player) {
        UUID uuid = player.getUniqueId();
        enabled.remove(uuid);
        disabled.remove(uuid);
    }

    public void spy(String message) {
        Runnable task = () -> {
            String formatted = CC.color(message);
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (isEnabled(player)) {
                    player.sendMessage(formatted);
                }
            }
        };
        if (Bukkit.isPrimaryThread()) {
            task.run();
        } else {
            Bukkit.getScheduler().runTask(plugin, task);
        }
    }
}
