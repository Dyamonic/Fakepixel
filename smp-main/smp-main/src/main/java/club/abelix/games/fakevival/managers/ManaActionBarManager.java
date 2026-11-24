package club.abelix.games.fakevival.managers;

import club.abelix.games.fakevival.data.DataManager;
import club.abelix.games.fakevival.mmo.MMOStatMap;
import club.abelix.games.fakevival.mmo.MMOStatType;
import club.abelix.games.fakevival.util.CC;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Function;

public final class ManaActionBarManager {
    private static final String DEFAULT_ACTIONBAR_FORMAT = "&c%current_mmo_hp%/%max_mmo_hp%❤  &a%defense%/%max_defense%❈ Defense  &b%current_mana%/%max_mana%✎ Mana";
    private final JavaPlugin plugin;
    private final DataManager dataManager;
    private final Map<UUID, Integer> mana = new HashMap<>();
    private Function<Player, MMOStatMap> statProvider;
    private Function<Player, Integer> currentHealthProvider;
    private Function<Player, Integer> maxHealthProvider;
    private BiFunction<Player, String, String> placeholderParser;
    private BukkitTask task;

    public ManaActionBarManager(JavaPlugin plugin) {
        this(plugin, null);
    }

    public ManaActionBarManager(JavaPlugin plugin, DataManager dataManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
    }

    public void start() {
        stop();
        if (!club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getBoolean("actionbar.enabled", true)) return;

        long interval = Math.max(1L, club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getLong("actionbar.update-interval-ticks", 10L));
        task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            mana.keySet().removeIf(uuid -> Bukkit.getPlayer(uuid) == null);
            for (Player player : Bukkit.getOnlinePlayers()) {
                tickMana(player);
                send(player);
            }
        }, 20L, interval);
    }

    public void stop() {
        if (task != null) task.cancel();
        task = null;
        mana.clear();
    }

    public void setMmoStatProvider(Function<Player, MMOStatMap> statProvider, Function<Player, Integer> currentHealthProvider, Function<Player, Integer> maxHealthProvider) {
        this.statProvider = statProvider;
        this.currentHealthProvider = currentHealthProvider;
        this.maxHealthProvider = maxHealthProvider;
    }

    public void setPlaceholderParser(BiFunction<Player, String, String> placeholderParser) {
        this.placeholderParser = placeholderParser;
    }

    public int getMana(Player player) {
        return mana.computeIfAbsent(player.getUniqueId(), ignored -> getMaxMana(player));
    }

    public int getMaxMana(Player player) {
        if (statProvider != null) {
            MMOStatMap stats = statProvider.apply(player);
            if (stats != null && stats.get(MMOStatType.MANA) > 0.0D) return Math.max(1, (int) Math.ceil(stats.get(MMOStatType.MANA)));
        }
        return Math.max(1, club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getInt("custom-items.mana.max",
                club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getInt("actionbar.mana.max", 100)) + getBonusMana(player));
    }

    public void setMana(Player player, int amount) {
        int max = getMaxMana(player);
        mana.put(player.getUniqueId(), Math.clamp(amount, 0, max));
    }

    public boolean spendMana(Player player, int amount) {
        if (amount <= 0) return true;
        int current = getMana(player);
        if (current < amount) return false;
        setMana(player, current - amount);
        send(player);
        return true;
    }

    public void clampMana(Player player) {
        setMana(player, getMana(player));
    }

    public void sendNow(Player player) {
        send(player);
    }

    private void tickMana(Player player) {
        int max = getMaxMana(player);
        int current = Math.min(getMana(player), max);
        int regen = Math.max(0, club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getInt("actionbar.mana.regen-per-update",
                Math.max(1, (int) Math.ceil(club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getDouble("custom-items.mana.regen-per-second", 2.0D) / 2.0D))));
        if (statProvider != null) {
            MMOStatMap stats = statProvider.apply(player);
            if (stats != null) regen += Math.max(0, (int) Math.floor(stats.get(MMOStatType.MANA_REGEN)));
        }
        mana.put(player.getUniqueId(), Math.min(max, current + regen));
    }

    private void send(Player player) {
        String format = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin)
                .getString("actionbar.format", DEFAULT_ACTIONBAR_FORMAT);
        String message = format
                .replace("%health%", String.valueOf(getHealth(player)))
                .replace("%max_health%", String.valueOf(getMaxHealth(player)))
                .replace("%current_mmo_hp%", String.valueOf(getHealth(player)))
                .replace("%max_mmo_hp%", String.valueOf(getMaxHealth(player)))
                .replace("%defense%", String.valueOf(getDefense(player)))
                .replace("%max_defense%", String.valueOf(getMaxDefense(player)))
                .replace("%smp_defense%", String.valueOf(getDefense(player)))
                .replace("%smp_max_defense%", String.valueOf(getMaxDefense(player)))
                .replace("%fakevival_defense%", String.valueOf(getDefense(player)))
                .replace("%fakevival_max_defense%", String.valueOf(getMaxDefense(player)))
                .replace("%mana%", String.valueOf(getMana(player)))
                .replace("%current_mana%", String.valueOf(getMana(player)))
                .replace("%max_mana%", String.valueOf(getMaxMana(player)))
                .replace("%level%", String.valueOf(getPlayerLevel(player)));
        if (placeholderParser != null) message = placeholderParser.apply(player, message);

        player.sendActionBar(LegacyComponentSerializer.legacySection().deserialize(CC.color(message)));
    }

    private int getHealth(Player player) {
        if (currentHealthProvider != null) return currentHealthProvider.apply(player);
        return (int) Math.ceil(Math.max(0.0D, player.getHealth()));
    }

    private int getBonusMana(Player player) {
        if (dataManager == null) return 0;
        double bonus = 0.0D;
        ConfigurationSection trees = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getConfigurationSection("skills.trees");
        if (trees == null) return 0;
        for (String skill : trees.getKeys(false)) {
            ConfigurationSection nodes = trees.getConfigurationSection(skill);
            if (nodes == null) continue;
            for (String node : nodes.getKeys(false)) {
                int level = dataManager.getData().getInt("players." + player.getUniqueId() + ".skill-tree." + skill.toLowerCase() + "." + node.toLowerCase(), 0);
                if (level <= 0) continue;
                bonus += level * Math.max(0.0D, nodes.getDouble(node + ".mana-per-level", 0.0D));
            }
        }
        int playerLevel = Math.max(1, dataManager.getData().getInt("players." + player.getUniqueId() + ".player-level.level", 1));
        ConfigurationSection levels = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getConfigurationSection("player-levels.levels");
        if (levels != null) {
            for (String key : levels.getKeys(false)) {
                try {
                    if (Integer.parseInt(key) <= playerLevel) bonus += Math.max(0.0D, levels.getDouble(key + ".stats.mana", 0.0D));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return (int) Math.floor(bonus);
    }

    private int getPlayerLevel(Player player) {
        if (dataManager == null) return 1;
        return Math.max(1, dataManager.getData().getInt("players." + player.getUniqueId() + ".player-level.level", 1));
    }

    private int getMaxHealth(Player player) {
        if (maxHealthProvider != null) return maxHealthProvider.apply(player);
        return 20;
    }

    private int getDefense(Player player) {
        if (statProvider == null) return 100;
        MMOStatMap stats = statProvider.apply(player);
        if (stats == null) return 0;
        return (int) Math.round(Math.max(0.0D, stats.get(MMOStatType.DEFENSE)));
    }

    private int getMaxDefense(Player player) {
        return getDefense(player);
    }
}
