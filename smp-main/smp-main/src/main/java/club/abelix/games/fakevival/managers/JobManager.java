package club.abelix.games.fakevival.managers;

import club.abelix.games.fakevival.data.DataManager;
import club.abelix.games.fakevival.util.CC;
import club.abelix.games.fakevival.util.SoundUtil;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class JobManager {
    private final JavaPlugin plugin;
    private final DataManager dataManager;
    private final Map<UUID, Set<String>> joinedJobs = new HashMap<>();
    private final ProgressBossBarManager bossBarManager;
    private final BattlePassManager battlePassManager;

    public JobManager(JavaPlugin plugin, DataManager dataManager, ProgressBossBarManager bossBarManager, BattlePassManager battlePassManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
        this.bossBarManager = bossBarManager;
        this.battlePassManager = battlePassManager;
    }

    private String playerPath(Player player) {
        return "players." + player.getUniqueId();
    }

    private int xpToLevel(int xp) {
        int perLevel = getXpPerLevel();
        return Math.max(1, (xp / perLevel) + 1);
    }

    private int getXpPerLevel() {
        return Math.max(1, club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getInt("job-leveling.xp-per-level", club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getInt("skills.xp-per-level", 100)));
    }

    public String getJob(Player player) {
        List<String> jobs = getJobs(player);
        return jobs.isEmpty() ? null : jobs.getFirst();
    }

    public List<String> getJobs(Player player) {
        Set<String> jobs = joinedJobs.get(player.getUniqueId());
        if (jobs == null) {
            jobs = new LinkedHashSet<>();
            List<String> saved = dataManager.getData().getStringList(playerPath(player) + ".jobs-joined");
            for (String job : saved) if (job != null && !job.isBlank()) jobs.add(job.toLowerCase());

            String oldJob = dataManager.getData().getString(playerPath(player) + ".job");
            if (oldJob != null && !oldJob.isBlank()) jobs.add(oldJob.toLowerCase());

            joinedJobs.put(player.getUniqueId(), jobs);
            saveJobs(player, jobs);
        }
        return new ArrayList<>(jobs);
    }

    public boolean hasJob(Player player, String job) {
        if (job == null) return false;
        return getJobs(player).stream().anyMatch(j -> j.equalsIgnoreCase(job));
    }

    public void setJob(Player player, String job) {
        joinJob(player, job);
    }

    public void joinJob(Player player, String job) {
        if(plugin instanceof club.abelix.games.fakevival.Game game&&!game.getNetworkService().isOverworld())return;
        if (job == null || job.isBlank()) return;
        Set<String> jobs = new LinkedHashSet<>(getJobs(player));
        jobs.add(job.toLowerCase());
        joinedJobs.put(player.getUniqueId(), jobs);
        saveJobs(player, jobs);
    }

    public void leaveJob(Player player, String job) {
        if(plugin instanceof club.abelix.games.fakevival.Game game&&!game.getNetworkService().isOverworld())return;
        if (job == null) return;
        Set<String> jobs = new LinkedHashSet<>(getJobs(player));
        jobs.removeIf(saved -> saved.equalsIgnoreCase(job));
        joinedJobs.put(player.getUniqueId(), jobs);
        saveJobs(player, jobs);
    }

    private void saveJobs(Player player, Set<String> jobs) {
        dataManager.getData().set(playerPath(player) + ".jobs-joined", new ArrayList<>(jobs));
        dataManager.getData().set(playerPath(player) + ".job", null);
        dataManager.save();
    }

    public String getJobsDisplay(Player player) {
        List<String> jobs = getJobs(player);
        if (jobs.isEmpty()) return "None";
        List<String> pretty = new ArrayList<>();
        for (String job : jobs) pretty.add(pretty(job));
        return String.join(", ", pretty);
    }

    public int getXp(Player player, String job) {
        if (job == null) return 0;
        return dataManager.getData().getInt(playerPath(player) + ".jobs." + job.toLowerCase() + ".xp", 0);
    }

    public int getLevel(Player player, String job) {
        return xpToLevel(getXp(player, job));
    }

    public int getMaxLevel(String job) {
        return Math.max(1, club.abelix.games.fakevival.config.RuntimeSettings.config(plugin)
                .getInt("jobs." + job.toLowerCase() + ".max-level",
                        club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getInt("job-leveling.max-level", 100)));
    }

    public void setLevel(Player player, String job, int level) {
        int target = Math.clamp(level, 1, getMaxLevel(job));
        dataManager.getData().set(playerPath(player) + ".jobs." + job.toLowerCase() + ".xp", (target - 1) * getXpPerLevel());
        dataManager.save();
    }

    public void addXp(Player player, String job, int amount) {
        if(plugin instanceof club.abelix.games.fakevival.Game game&&!game.getNetworkService().isOverworld())return;
        if (!club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getBoolean("jobs-enabled", true) || job == null) return;
        job = job.toLowerCase();
        int oldLevel = getLevel(player, job);
        int finalAmount = applyStoreBooster(player, Math.max(0, amount));
        int xp = getXp(player, job) + finalAmount;
        dataManager.getData().set(playerPath(player) + ".jobs." + job + ".xp", xp);
        dataManager.save();
        if (battlePassManager != null) battlePassManager.addJobXp(player, finalAmount);
        int newLevel = getLevel(player, job);
        int bossBarNeeded = getBossBarXpNeeded(newLevel);
        bossBarManager.showJob(player, job, newLevel, xp, Math.min(bossBarNeeded, getBossBarXpIntoLevel(xp, newLevel)), bossBarNeeded);
        if (newLevel > oldLevel) {
            playLevelSound(player);
            player.sendMessage(CC.color(club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString("messages.job-level-up", "&aYour %job% job is now level %level%!")
                    .replace("%job%", pretty(job))
                    .replace("%level%", String.valueOf(newLevel))));
        }
    }

    private void playLevelSound(Player player) {
        if (!club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getBoolean("sounds.job-level-up" + ".enabled", true)) return;
        try {
            Sound sound = SoundUtil.parse(club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString("sounds.job-level-up" + ".sound", "ENTITY_PLAYER_LEVELUP"));
            if (sound != null) player.playSound(player.getLocation(), sound, (float) club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getDouble("sounds.job-level-up" + ".volume", 1.0D), (float) club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getDouble("sounds.job-level-up" + ".pitch", 1.0D));
        } catch (Exception ignored) {
        }
    }

    public boolean jobExists(String job) {
        ConfigurationSection section = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getConfigurationSection("jobs");
        return section == null || !section.contains(job.toLowerCase());
    }

    private int applyStoreBooster(Player player, int amount) {
        String path = playerPath(player) + ".store.boosters.jobs";
        long expiresAt = dataManager.getData().getLong(path + ".expires-at", 0L);
        if (expiresAt <= System.currentTimeMillis()) return amount;
        double multiplier = Math.max(1.0D, dataManager.getData().getDouble(path + ".multiplier", 1.0D));
        return (int) Math.round(amount * multiplier);
    }

    private int getBossBarXpNeeded(int level) {
        int base = getXpPerLevel();
        int growth = Math.max(0, club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getInt("job-leveling.xp-per-level-growth",
                club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getInt("skills.xp-per-level-growth", 25)));
        return base + Math.max(0, level - 1) * growth;
    }

    private int getBossBarXpIntoLevel(int totalXp, int level) {
        int base = getXpPerLevel();
        int rawIntoLevel = Math.max(0, totalXp - Math.max(0, level - 1) * base);
        return (int) Math.round((double) Math.min(rawIntoLevel, base) / (double) base * getBossBarXpNeeded(level));
    }

    public ConfigurationSection getJobsSection() {
        return club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getConfigurationSection("jobs");
    }

    private String pretty(String text) {
        if (text == null || text.isEmpty()) return "None";
        return text.substring(0, 1).toUpperCase() + text.substring(1).toLowerCase();
    }
}
