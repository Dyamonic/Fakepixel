package net.fakepixel.housing.tablist;

import net.fakepixel.housing.HousingPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.lang.reflect.Method;
import java.util.List;

public class TablistService {
    private final HousingPlugin plugin;
    private int taskId = -1;

    public TablistService(HousingPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        stop();
        if (!plugin.getStaticConfig().getBoolean("tablist.enabled", true)) return;
        long ticks = Math.max(1L, plugin.getStaticConfig().getLong("tablist.update-ticks", 40L));
        taskId = new BukkitRunnable() {
            @Override
            public void run() {
                updateAll();
            }
        }.runTaskTimer(plugin, 20L, ticks).getTaskId();
        updateAll();
    }

    public void stop() {
        if (taskId != -1) Bukkit.getScheduler().cancelTask(taskId);
        taskId = -1;
    }

    public void reload() {
        start();
    }

    public void updateAll() {
        if (!plugin.getStaticConfig().getBoolean("tablist.enabled", true)) return;
        for (Player player : Bukkit.getOnlinePlayers()) update(player);
    }

    public void update(Player player) {
        if (player == null || !player.isOnline()) return;
        String header = join(plugin.getStaticConfig().getStringList("tablist.header"));
        String footer = join(plugin.getStaticConfig().getStringList("tablist.footer"));
        header = plugin.applyPlaceholders(player, header);
        footer = plugin.applyPlaceholders(player, footer);
        try {
            Method method = player.getClass().getMethod("setPlayerListHeaderFooter", String.class, String.class);
            method.invoke(player, header, footer);
        } catch (Throwable ignored) {
        }
    }

    private String join(List<String> lines) {
        if (lines == null || lines.isEmpty()) return "";
        StringBuilder builder = new StringBuilder();
        for (String line : lines) {
            if (builder.length() > 0) builder.append("\n");
            builder.append(line == null ? "" : line);
        }
        return builder.toString();
    }
}
