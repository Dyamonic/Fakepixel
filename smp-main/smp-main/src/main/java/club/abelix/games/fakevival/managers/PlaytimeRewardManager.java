package club.abelix.games.fakevival.managers;

import club.abelix.games.fakevival.data.DataManager;
import club.abelix.games.fakevival.util.CC;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

public final class PlaytimeRewardManager {
    private final JavaPlugin plugin;
    private final DataManager dataManager;
    private int taskId = -1;

    public PlaytimeRewardManager(JavaPlugin plugin, DataManager dataManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
    }

    public void start() {
        if (!club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getBoolean("playtime-rewards.enabled", true)) return;
        long interval = Math.max(20L, club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getLong("playtime-rewards.track-interval-ticks", 1200L));
        taskId = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            long seconds = interval / 20L;
            for (Player player : Bukkit.getOnlinePlayers()) addSeconds(player, seconds);
        }, interval, interval).getTaskId();
    }

    public void stop() {
        if (taskId != -1) Bukkit.getScheduler().cancelTask(taskId);
    }

    public int getPlaytimeSeconds(Player player) {
        return dataManager.getData().getInt(path(player) + ".playtime-seconds", 0);
    }

    public int getClaimableDays(Player player) {
        int secondsPerDay = Math.max(1, club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getInt("playtime-rewards.seconds-per-reward-day", 86400));
        return Math.min(30, getPlaytimeSeconds(player) / secondsPerDay);
    }

    public boolean canClaim(Player player, int day) {
        return day >= 1 && day <= 30 && getClaimableDays(player) >= day && !isClaimed(player, day);
    }

    public boolean isClaimed(Player player, int day) {
        return dataManager.getData().getBoolean(path(player) + ".claimed." + day, false);
    }

    public boolean claim(Player player, int day) {
        if (!canClaim(player, day)) return false;
        List<String> commands = rewardCommands(day);
        if (commands.isEmpty()) return false;
        for (String command : commands) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replace("%player%", player.getName()).replace("%day%", String.valueOf(day)).replace("%month%", monthKey()));
        }
        dataManager.getData().set(path(player) + ".claimed." + day, true);
        dataManager.save();
        player.sendMessage(CC.color(club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString("playtime-rewards.messages.claimed", "&aClaimed playtime reward day &f%day%&a.")
                .replace("%day%", String.valueOf(day))));
        return true;
    }

    public String monthKey() {
        return YearMonth.now(ZoneId.systemDefault()).toString();
    }

    public String activeSet() {
        String exact = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString("playtime-rewards.rotation.months." + monthKey());
        if (exact != null && club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).isConfigurationSection("playtime-rewards.reward-sets." + exact)) return exact;
        String monthName = YearMonth.now(ZoneId.systemDefault()).getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH).toLowerCase(Locale.ENGLISH);
        String named = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString("playtime-rewards.rotation.months." + monthName);
        if (named != null && club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).isConfigurationSection("playtime-rewards.reward-sets." + named)) return named;
        String fallback = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString("playtime-rewards.rotation.default-set", "default");
        return club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).isConfigurationSection("playtime-rewards.reward-sets." + fallback) ? fallback : "default";
    }

    public String rewardDisplay(int day) {
        return CC.color(club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString(rewardPath(day) + ".display", "&aDay " + day + " Reward"));
    }

    public Material rewardMaterial(int day) {
        Material material = Material.matchMaterial(club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString(rewardPath(day) + ".material", day % 5 == 0 ? "GOLD_INGOT" : "CHEST"));
        return material == null ? Material.CHEST : material;
    }

    public List<String> rewardLore(int day) {
        List<String> lore = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getStringList(rewardPath(day) + ".lore");
        if (!lore.isEmpty()) return CC.colorList(lore);
        return CC.colorList(rewardCommands(day).stream().map(command -> "&8- &f" + describe(command)).toList());
    }

    public List<String> rewardCommands(int day) {
        return club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getStringList(rewardPath(day) + ".commands");
    }

    private void addSeconds(Player player, long seconds) {
        dataManager.getData().set(path(player) + ".playtime-seconds", getPlaytimeSeconds(player) + seconds);
    }

    private String rewardPath(int day) {
        String active = activeSet();
        String path = "playtime-rewards.reward-sets." + active + ".rewards." + day;
        if (club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).isConfigurationSection(path)) return path;
        return "playtime-rewards.reward-sets.default.rewards." + day;
    }

    private String path(Player player) {
        return "players." + player.getUniqueId() + ".playtime-rewards.months." + monthKey();
    }

    private String describe(String command) {
        String normalized = command == null ? "" : command.replace("%player%", "").trim();
        String lower = normalized.toLowerCase(Locale.ENGLISH);
        if (lower.startsWith("coins give ")) {
            String[] parts = normalized.split("\\s+");
            if (parts.length >= 3) return parts[2] + " coins";
        }
        if (lower.startsWith("give ")) {
            String[] parts = normalized.split("\\s+");
            if (parts.length >= 3) return parts[2] + "x " + parts[1].replace('_', ' ').toLowerCase(Locale.ENGLISH);
        }
        return normalized;
    }
}
