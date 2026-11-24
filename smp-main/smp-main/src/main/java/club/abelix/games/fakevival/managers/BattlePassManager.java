package club.abelix.games.fakevival.managers;

import club.abelix.games.fakevival.Game;
import club.abelix.games.fakevival.data.DataManager;
import club.abelix.games.fakevival.hooks.AbelixHook;
import club.abelix.games.fakevival.util.CC;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public final class BattlePassManager {
    private final JavaPlugin plugin;
    private final DataManager dataManager;
    private final AbelixHook abelixHook;
    private FileConfiguration config;

    public BattlePassManager(JavaPlugin plugin, DataManager dataManager, AbelixHook abelixHook) {
        this.plugin = plugin;
        this.dataManager = dataManager;
        this.abelixHook = abelixHook;
    }

    public void setup() {
        config = plugin instanceof Game game
                ? game.getStaticGameRegistries().battlepass().config()
                : new club.abelix.games.fakevival.registries.BattlepassRegistry(plugin).config();
        addDefaults();
    }

    public void reload() {
        setup();
    }

    private void addDefaults() {
        config.addDefault("enabled", true);
        config.addDefault("xp-per-level", 2500);
        config.addDefault("max-level", 100);
        config.addDefault("buy-level-cost-gold", 250);
        config.addDefault("premium-cost-gold", 1500);
        config.addDefault("menu.levels-per-page", 9);
        config.addDefault("menu.locked-material", "MINECART");
        config.addDefault("menu.unlocked-material", "CHEST_MINECART");
        config.addDefault("menu.premium-glow", true);
        config.addDefault("xp-from.skills-multiplier", 1.0D);
        config.addDefault("xp-from.jobs-multiplier", 1.0D);
        config.addDefault("rewards.1.free.commands", List.of("coins give %player% 125"));
        config.addDefault("rewards.1.premium.commands", List.of("coins give %player% 300"));
        config.addDefault("rewards.1.display", "&aBattlePass Reward &7#1");
        for (int level = 1; level <= 100; level++) {
            String display = level % 9 == 0 ? "&6Milestone Reward" : "&aBattlePass Reward";
            String freeCommand = "coins give %player% " + (100 + (level * 25));
            String premiumCommand = level % 5 == 0 ? "give %player% diamond 1" : "coins give %player% " + (250 + (level * 50));
            config.addDefault("rewards." + level + ".display", display + " &7#" + level);
            config.addDefault("rewards." + level + ".material", level % 5 == 0 ? "CHEST_MINECART" : "MINECART");
            config.addDefault("rewards." + level + ".free.commands", List.of(freeCommand));
            config.addDefault("rewards." + level + ".premium.commands", List.of(premiumCommand));
            config.addDefault("rewards." + level + ".free.lore", List.of("&8- &f" + describeReward(freeCommand)));
            config.addDefault("rewards." + level + ".premium.lore", List.of("&8- &f" + describeReward(premiumCommand)));
        }
        config.options().copyDefaults(true);
    }

    public String describeReward(String command) {
        String normalized = command == null ? "" : command.replace("%player%", "").trim();
        String lower = normalized.toLowerCase();
        if (lower.startsWith("coins give ")) {
            String[] parts = normalized.split("\\s+");
            if (parts.length >= 3) return parts[2] + " coins";
        }
        if (lower.startsWith("give ")) {
            String[] parts = normalized.split("\\s+");
            if (parts.length >= 3) {
                String material = parts[1].replace('_', ' ').toLowerCase();
                String amount = parts[2];
                return amount + "x " + material;
            }
        }
        return normalized;
    }

    public void save() {
    }

    public FileConfiguration getConfig() {
        if (config == null) setup();
        return config;
    }

    private String path(Player player) {
        return "players." + player.getUniqueId() + ".battlepass";
    }

    public int getXp(Player player) {
        return dataManager.getData().getInt(path(player) + ".xp", 0);
    }

    public int getLevel(Player player) {
        return Math.clamp(getConfig().getInt("max-level", 100), 1, (getXp(player) / getXpPerLevel()) + 1);
    }

    public int getXpPerLevel() {
        return Math.max(1, getConfig().getInt("xp-per-level", 2500));
    }

    public boolean hasPremium(Player player) {
        return dataManager.getData().getBoolean(path(player) + ".premium", false);
    }

    public void setPremium(Player player, boolean premium) {
        dataManager.getData().set(path(player) + ".premium", premium);
        dataManager.save();
    }

    public boolean buyPremium(Player player) {
        if (hasPremium(player)) return false;
        int cost = Math.max(0, getConfig().getInt("premium-cost-gold", 1500));
        if (cost > 0 && !abelixHook.takeGold(player, cost)) return false;
        setPremium(player, true);
        giveRewards(player, 1, getLevel(player));
        return true;
    }

    public boolean isFreeUnlocked(Player player, int level) {
        return level <= getLevel(player);
    }

    public boolean isPremiumUnlocked(Player player, int level) {
        return hasPremium(player) && level <= getLevel(player);
    }

    public int getTotalPages() {
        int perPage = Math.max(1, getConfig().getInt("menu.levels-per-page", 9));
        int count = Math.max(1, rewardLevels().size());
        return Math.max(1, (int) Math.ceil(count / (double) perPage));
    }

    public void addSkillXp(Player player, int skillXp) {
        addXp(player, (int) Math.round(skillXp * getConfig().getDouble("xp-from.skills-multiplier", 1.0D)));
    }

    public void addJobXp(Player player, int jobXp) {
        addXp(player, (int) Math.round(jobXp * getConfig().getDouble("xp-from.jobs-multiplier", 1.0D)));
    }

    public void addPlayXp(Player player, int amount) {
        addXp(player, amount);
    }

    public void addXp(Player player, int amount) {
        if (!getConfig().getBoolean("enabled", true) || amount <= 0) return;
        int oldLevel = getLevel(player);
        dataManager.getData().set(path(player) + ".xp", getXp(player) + amount);
        dataManager.save();
        int newLevel = getLevel(player);
        if (newLevel > oldLevel) {
            player.sendMessage(CC.color("&dBattlePass &8» &aYou reached level &f" + newLevel + "&a!"));
            giveRewards(player, oldLevel + 1, newLevel);
        }
    }

    public boolean buyLevels(Player player, int levels) {
        if (levels <= 0) return false;
        int cost = levels * getConfig().getInt("buy-level-cost-gold", 250);
        if (!abelixHook.takeGold(player, cost)) return false;
        addXp(player, levels * getXpPerLevel());
        return true;
    }

    public void giveRewards(Player player, int fromLevel, int toLevel) {
        for (int level = fromLevel; level <= toLevel; level++) {
            runCommands(player, getConfig().getStringList("rewards." + level + ".free.commands"));
            if (hasPremium(player)) runCommands(player, getConfig().getStringList("rewards." + level + ".premium.commands"));
        }
    }

    public List<Integer> rewardLevels() {
        List<Integer> levels = new ArrayList<>();
        org.bukkit.configuration.ConfigurationSection rewards = getConfig().getConfigurationSection("rewards");
        if (rewards == null) return levels;
        for (String key : rewards.getKeys(false)) {
            try { levels.add(Integer.parseInt(key)); } catch (Exception ignored) {}
        }
        levels.sort(Integer::compareTo);
        return levels;
    }

    public Material materialForLevel(int level) {
        String mat = getConfig().getString("rewards." + level + ".material", level % 5 == 0 ? "GOLD_INGOT" : "BOOK");
        try { return Material.valueOf(mat.toUpperCase()); } catch (Exception e) { return Material.BOOK; }
    }

    private void runCommands(Player player, List<String> commands) {
        for (String command : commands) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replace("%player%", player.getName()).replace("%level%", String.valueOf(getLevel(player))));
        }
    }
}
