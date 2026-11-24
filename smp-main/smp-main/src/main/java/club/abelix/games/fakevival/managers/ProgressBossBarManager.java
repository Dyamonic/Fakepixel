package club.abelix.games.fakevival.managers;

import club.abelix.games.fakevival.util.CC;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;

public final class ProgressBossBarManager {
    private final JavaPlugin plugin;
    private final Map<String, BossBar> bars = new HashMap<>();
    private final Map<String, BukkitTask> removeTasks = new HashMap<>();

    public ProgressBossBarManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void showSkill(Player player, String skill, int level, int currentXp, int xpIntoLevel, int xpNeeded) {
        if (!club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getBoolean("bossbar.skills.enabled", true)) return;
        String title = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString("bossbar.skills.title", "&b%skill% &7Level &a%level% &8- &e%xp%&7/&e%needed% XP")
                .replace("%skill%", pretty(skill))
                .replace("%level%", String.valueOf(level))
                .replace("%total_xp%", String.valueOf(currentXp))
                .replace("%xp%", String.valueOf(xpIntoLevel))
                .replace("%needed%", String.valueOf(xpNeeded));
        show(player, "skill-" + skill.toLowerCase(), title, progress(xpIntoLevel, xpNeeded), getColor("bossbar.skills.color", BarColor.BLUE));
    }

    public void showJob(Player player, String job, int level, int currentXp, int xpIntoLevel, int xpNeeded) {
        if (!club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getBoolean("bossbar.jobs.enabled", true)) return;
        String title = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString("bossbar.jobs.title", "&6%job% Job &7Level &a%level% &8- &e%xp%&7/&e%needed% XP")
                .replace("%job%", pretty(job))
                .replace("%level%", String.valueOf(level))
                .replace("%total_xp%", String.valueOf(currentXp))
                .replace("%xp%", String.valueOf(xpIntoLevel))
                .replace("%needed%", String.valueOf(xpNeeded));
        show(player, "job-" + job.toLowerCase(), title, progress(xpIntoLevel, xpNeeded), getColor("bossbar.jobs.color", BarColor.YELLOW));
    }

    private void show(Player player, String type, String title, double progress, BarColor color) {
        String key = player.getUniqueId() + ":" + type;
        BossBar bar = bars.get(key);
        if (bar == null) {
            bar = Bukkit.createBossBar(CC.color(title), color, BarStyle.SOLID);
            bars.put(key, bar);
        }
        bar.setTitle(CC.color(title));
        bar.setColor(color);
        bar.setProgress(Math.clamp(progress, 0.0D, 1.0D));
        if (!bar.getPlayers().contains(player)) bar.addPlayer(player);

        BukkitTask old = removeTasks.remove(key);
        if (old != null) old.cancel();
        int ticks = Math.max(20, club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getInt("bossbar.hide-after-ticks", 60));
        removeTasks.put(key, Bukkit.getScheduler().runTaskLater(plugin, () -> remove(key), ticks));
    }

    public void removePlayer(Player player) {
        String prefix = player.getUniqueId() + ":";
        for (String key : new java.util.ArrayList<>(bars.keySet())) {
            if (key.startsWith(prefix)) remove(key);
        }
    }

    public void stop() {
        for (BukkitTask task : removeTasks.values()) task.cancel();
        removeTasks.clear();
        for (BossBar bar : bars.values()) bar.removeAll();
        bars.clear();
    }

    private void remove(String key) {
        BukkitTask task = removeTasks.remove(key);
        if (task != null) task.cancel();
        BossBar bar = bars.remove(key);
        if (bar != null) bar.removeAll();
    }

    private double progress(int xp, int needed) {
        if (needed <= 0) return 1.0D;
        return Math.clamp((double) xp / (double) needed, 0.0D, 1.0D);
    }

    private BarColor getColor(String path, BarColor def) {
        try {
            return BarColor.valueOf(club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString(path, def.name()).toUpperCase());
        } catch (Exception ignored) {
            return def;
        }
    }

    private String pretty(String text) {
        if (text == null || text.isEmpty()) return "None";
        return text.substring(0, 1).toUpperCase() + text.substring(1).toLowerCase();
    }
}
