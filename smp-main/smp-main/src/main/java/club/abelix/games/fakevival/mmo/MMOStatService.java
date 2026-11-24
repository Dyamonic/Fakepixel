package club.abelix.games.fakevival.mmo;

import club.abelix.games.fakevival.managers.CustomItemManager;
import club.abelix.games.fakevival.managers.ManaActionBarManager;
import club.abelix.games.fakevival.managers.SkillManager;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.List;
import java.util.HashSet;
import java.util.Set;

public final class MMOStatService {
    private static final double VANILLA_MAX_HEALTH = 20.0D;

    private final JavaPlugin plugin;
    private final CustomItemManager customItemManager;
    private final SkillManager skillManager;
    private final ManaActionBarManager manaActionBarManager;
    private final NamespacedKey armorSetKey;
    private final NamespacedKey armorPieceKey;
    private final Map<UUID, Double> currentHealth = new ConcurrentHashMap<>();
    private final Map<UUID, Double> lastMaxHealth = new ConcurrentHashMap<>();
    private final Map<UUID, MMOStatMap> totalStats = new ConcurrentHashMap<>();
    private BukkitTask refreshTask;
    private Objective belowNameHealth;
    private final Map<UUID, Integer> lastBelowNameHealth = new ConcurrentHashMap<>();
    private BiFunction<Player, MMOStatType, Double> eventStatProvider = (player, stat) -> 0.0D;
    private BiFunction<Player, MMOStatType, Double> levelStatProvider = (player, stat) -> 0.0D;
    private Function<Player, List<ItemStack>> accessoryProvider = player -> List.of();

    public MMOStatService(JavaPlugin plugin, CustomItemManager customItemManager, SkillManager skillManager, ManaActionBarManager manaActionBarManager) {
        this.plugin = plugin;
        this.customItemManager = customItemManager;
        this.skillManager = skillManager;
        this.manaActionBarManager = manaActionBarManager;
        this.armorSetKey = new NamespacedKey(plugin, "armor_set_id");
        this.armorPieceKey = new NamespacedKey(plugin, "armor_piece");
    }

    public void start() {
        stop();
        ensureBelowNameObjective();
        refreshTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : plugin.getServer().getOnlinePlayers()) refreshPlayerMMOStats(player);
        }, 20L, 20L);
    }

    public void stop() {
        if (refreshTask != null) refreshTask.cancel();
        refreshTask = null;
        currentHealth.clear();
        lastMaxHealth.clear();
        totalStats.clear();
        lastBelowNameHealth.clear();
        if (belowNameHealth != null) {
            try { belowNameHealth.unregister(); } catch (IllegalStateException ignored) { }
            belowNameHealth = null;
        }
    }

    public MMOStatMap getTotalStats(Player player) {
        if (player == null) return MMOStatMap.empty();
        return totalStats.computeIfAbsent(player.getUniqueId(), ignored -> calculateTotalStats(player));
    }

    private MMOStatMap calculateTotalStats(Player player) {
        MMOStatMap total = MMOStatMap.empty();
        total.add(MMOStatType.HEALTH, baseHealth());
        total.add(MMOStatType.MANA, baseMana());
        total.add(MMOStatType.DEFENSE, baseDefense());
        addSkillAndLevelStats(player, total);
        for (MMOStatType type : MMOStatType.values()) total.add(type, Math.max(0.0D, levelStatProvider.apply(player, type)));
        addItemStats(player, total, true);
        MMOStatMap consumableStats = customItemManager.getTemporaryConsumableStats(player);
        for (MMOStatType type : MMOStatType.values()) total.add(type, consumableStats.get(type));
        for (MMOStatType type : new MMOStatType[]{MMOStatType.MINING_FORTUNE, MMOStatType.SEA_CREATURE_CHANCE, MMOStatType.TREASURE_CHANCE}) {
            total.add(type, Math.max(0.0D, eventStatProvider.apply(player, type)));
        }
        return total;
    }

    public void setEventStatProvider(BiFunction<Player, MMOStatType, Double> eventStatProvider) {
        this.eventStatProvider = eventStatProvider == null ? (player, stat) -> 0.0D : eventStatProvider;
        totalStats.clear();
    }

    public void setLevelStatProvider(BiFunction<Player, MMOStatType, Double> levelStatProvider) {
        this.levelStatProvider = levelStatProvider == null ? (player, stat) -> 0.0D : levelStatProvider;
        totalStats.clear();
    }

    public void setAccessoryProvider(Function<Player, List<ItemStack>> accessoryProvider) {
        this.accessoryProvider = accessoryProvider == null ? player -> List.of() : accessoryProvider;
        totalStats.clear();
    }

    public MMOStatMap getEquipmentStats(Player player, boolean includeHeld) {
        MMOStatMap total = MMOStatMap.empty();
        addItemStats(player, total, includeHeld);
        return total;
    }

    public void refreshPlayerMMOStats(Player player) {
        if (player == null || !player.isOnline()) return;
        totalStats.put(player.getUniqueId(), calculateTotalStats(player));
        refreshHealthCache(player);
        keepVanillaMaxHealthNormal(player);
        syncVanillaHealthBar(player);
        manaActionBarManager.clampMana(player);
        manaActionBarManager.sendNow(player);
        updateBelowNameHealth(player);
    }

    public void removePlayer(Player player) {
        if (player == null) return;
        UUID uuid = player.getUniqueId();
        totalStats.remove(uuid);
        currentHealth.remove(uuid);
        lastMaxHealth.remove(uuid);
        lastBelowNameHealth.remove(uuid);
    }

    public int currentMmoHealth(Player player) {
        refreshHealthCache(player);
        return (int) Math.ceil(Math.max(0.0D, currentHealth.getOrDefault(player.getUniqueId(), maxMmoHealthValue(player))));
    }

    public int maxMmoHealth(Player player) {
        return (int) Math.ceil(maxMmoHealthValue(player));
    }

    public MMOSnapshot getSnapshot(Player player) {
        if (player == null || !player.isOnline()) return MMOSnapshot.EMPTY;
        MMOStatMap stats = getTotalStats(player);
        int maxHealth = (int) Math.ceil(Math.max(1.0D, stats.get(MMOStatType.HEALTH)));
        int health = (int) Math.ceil(Math.clamp(currentHealth.getOrDefault(player.getUniqueId(), (double) maxHealth), 0.0D, maxHealth));
        int defense = (int) Math.round(Math.max(0.0D, stats.get(MMOStatType.DEFENSE)));
        int maxMana = manaActionBarManager.getMaxMana(player);
        return new MMOSnapshot(health, maxHealth, defense, defense, manaActionBarManager.getMana(player), maxMana);
    }

    public MMOSnapshot snapshot(Player player) { return getSnapshot(player); }

    public record MMOSnapshot(int health, int maxHealth, int defense, int maxDefense, int mana, int maxMana) {
        private static final MMOSnapshot EMPTY = new MMOSnapshot(0, 0, 0, 0, 0, 0);
    }

    public void applyMmoDamage(Player player, double amount) {
        if (player == null || amount <= 0.0D) return;
        refreshHealthCache(player);
        UUID uuid = player.getUniqueId();
        double next = currentHealth.getOrDefault(uuid, maxMmoHealthValue(player)) - amount;
        currentHealth.put(uuid, Math.max(0.0D, next));
        if (next <= 0.0D) {
            keepVanillaMaxHealthNormal(player);
            updateBelowNameHealth(player);
            player.setHealth(0.0D);
            return;
        }
        syncVanillaHealthBar(player);
        manaActionBarManager.sendNow(player);
        updateBelowNameHealth(player);
    }

    /** Central environmental/custom damage entry point for traps and other non-attacker hazards. */
    public double applyEnvironmentalDamage(Player player, double rawAmount, MMODamageSourceType sourceType) {
        if (player == null || rawAmount <= 0.0D || player.isDead()) return 0.0D;
        double defense = getTotalStats(player).get(MMOStatType.DEFENSE);
        double reduced = CombatBalance.reduce(plugin, rawAmount, defense);
        PotionEffect resistance = player.getPotionEffect(PotionEffectType.RESISTANCE);
        if (resistance != null) reduced *= Math.max(0.2D, 1.0D - 0.2D * (resistance.getAmplifier() + 1));
        double applied = Math.max(0.0D, reduced);
        MMODamageSourceTracker.runAs(sourceType == null ? MMODamageSourceType.ABILITY : sourceType,
                () -> applyMmoDamage(player, applied));
        return applied;
    }

    public double healMmoHealth(Player player, double amount) {
        if (player == null || amount <= 0.0D) return 0.0D;
        refreshHealthCache(player);
        UUID uuid = player.getUniqueId();
        double max = maxMmoHealthValue(player);
        double before = currentHealth.getOrDefault(uuid, max);
        double after = Math.min(max, before + amount);
        currentHealth.put(uuid, after);
        syncVanillaHealthBar(player);
        manaActionBarManager.sendNow(player);
        updateBelowNameHealth(player);
        return after - before;
    }

    public void setMmoHealth(Player player, double amount) {
        if (player == null) return;
        double max = maxMmoHealthValue(player);
        currentHealth.put(player.getUniqueId(), Math.clamp(amount, 0.0D, max));
        lastMaxHealth.put(player.getUniqueId(), max);
        syncVanillaHealthBar(player);
        manaActionBarManager.sendNow(player);
        updateBelowNameHealth(player);
    }

    public double healFromVanillaAmount(Player player, double vanillaAmount) {
        if (player == null || vanillaAmount <= 0.0D) return 0.0D;
        double max = maxMmoHealthValue(player);
        return healMmoHealth(player, vanillaAmount / VANILLA_MAX_HEALTH * max);
    }

    public void resetMmoHealth(Player player) {
        if (player == null) return;
        double max = maxMmoHealthValue(player);
        currentHealth.put(player.getUniqueId(), max);
        lastMaxHealth.put(player.getUniqueId(), max);
        keepVanillaMaxHealthNormal(player);
        syncVanillaHealthBar(player);
        manaActionBarManager.sendNow(player);
        updateBelowNameHealth(player);
    }

    private void refreshHealthCache(Player player) {
        UUID uuid = player.getUniqueId();
        double max = maxMmoHealthValue(player);
        double previousMax = lastMaxHealth.getOrDefault(uuid, max);
        double current = currentHealth.getOrDefault(uuid, max);
        if (previousMax > 0.0D && Math.abs(previousMax - max) > 0.001D) {
            double percent = Math.clamp(current / previousMax, 0.0D, 1.0D);
            current = max * percent;
        }
        currentHealth.put(uuid, Math.clamp(current, 0.0D, max));
        lastMaxHealth.put(uuid, max);
    }

    private double maxMmoHealthValue(Player player) {
        return Math.max(1.0D, getTotalStats(player).get(MMOStatType.HEALTH));
    }

    private void keepVanillaMaxHealthNormal(Player player) {
        AttributeInstance attribute = player.getAttribute(Attribute.MAX_HEALTH);
        if (attribute != null && Math.abs(attribute.getBaseValue() - VANILLA_MAX_HEALTH) > 0.001D) {
            attribute.setBaseValue(VANILLA_MAX_HEALTH);
        }
    }

    private void syncVanillaHealthBar(Player player) {
        AttributeInstance attribute = player.getAttribute(Attribute.MAX_HEALTH);
        double vanillaMax = attribute == null ? VANILLA_MAX_HEALTH : Math.max(1.0D, attribute.getValue());
        double max = maxMmoHealthValue(player);
        double current = currentHealth.getOrDefault(player.getUniqueId(), max);
        double percent = Math.clamp(current / max, 0.0D, 1.0D);
        double visual = Math.clamp(vanillaMax * percent, 0.5D, vanillaMax);
        if (player.getHealth() > 0.0D) player.setHealth(visual);
    }

    private void ensureBelowNameObjective() {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        Objective existing = scoreboard.getObjective("smp_health_below");
        belowNameHealth = existing == null
                ? scoreboard.registerNewObjective("smp_health_below", Criteria.DUMMY, ChatColor.RED + "❤") : existing;
        belowNameHealth.setDisplayName(ChatColor.RED + "❤");
        belowNameHealth.setDisplaySlot(DisplaySlot.BELOW_NAME);
    }

    public void updateBelowNameHealth(Player player) {
        if (player == null || !player.isOnline()) return;
        if (belowNameHealth == null) ensureBelowNameObjective();
        int health = Math.max(0, currentMmoHealth(player));
        if (health == lastBelowNameHealth.getOrDefault(player.getUniqueId(), Integer.MIN_VALUE)) return;
        lastBelowNameHealth.put(player.getUniqueId(), health);
        belowNameHealth.getScore(player.getName()).setScore(health);
    }

    private void addSkillAndLevelStats(Player player, MMOStatMap total) {
        // PlayerLevel is supplied separately by levelStatProvider. Using getTotalStatBonus here
        // counted every PlayerLevel contribution twice.
        total.add(MMOStatType.HEALTH, skillManager.getStatBonus(player, "health"));
        total.add(MMOStatType.MANA, skillManager.getStatBonus(player, "mana"));
        total.add(MMOStatType.DAMAGE, skillManager.getStatBonus(player, "damage"));
        total.add(MMOStatType.DEFENSE, skillManager.getStatBonus(player, "defense"));
        total.add(MMOStatType.SPEED, skillManager.getStatBonus(player, "agility"));
    }

    private void addItemStats(Player player, MMOStatMap total, boolean includeHeld) {
        if (includeHeld) total.addAll(customItemManager.getMmoStats(player, player.getInventory().getItemInMainHand()));
        total.addAll(customItemManager.getMmoStats(player, player.getInventory().getItemInOffHand()));
        for (ItemStack armor : player.getInventory().getArmorContents()) total.addAll(customItemManager.getMmoStats(player, armor));
        addArmorSetBonuses(player, total);
        Set<String> countedIds = new HashSet<>();
        for (ItemStack item : accessoryProvider.apply(player)) {
            MMOItemType type = customItemManager.getMmoItemType(item);
            if (type != MMOItemType.ACCESSORY && type != MMOItemType.RING && type != MMOItemType.NECKLACE
                    && type != MMOItemType.TRINKET && type != MMOItemType.CHARM) continue;
            String id = customItemManager.getItemId(item);
            if (id != null && !countedIds.add(id.toLowerCase(java.util.Locale.ROOT))) continue;
            total.addAll(customItemManager.getMmoStats(player, item));
        }
    }

    private void addArmorSetBonuses(Player player, MMOStatMap total) {
        Set<String> futuristicPieces = new HashSet<>();
        for (ItemStack armor : player.getInventory().getArmorContents()) {
            if (armor == null || armor.getType().isAir() || !armor.hasItemMeta()) continue;
            var pdc = armor.getItemMeta().getPersistentDataContainer();
            if (!"futuristic".equalsIgnoreCase(pdc.get(armorSetKey, PersistentDataType.STRING))) continue;
            String piece = pdc.get(armorPieceKey, PersistentDataType.STRING);
            if (piece != null) futuristicPieces.add(piece.toUpperCase(java.util.Locale.ROOT));
        }
        if (futuristicPieces.containsAll(Set.of("HELMET", "CHESTPLATE", "LEGGINGS", "BOOTS"))) {
            total.add(MMOStatType.HEALTH, 20.0D);
            total.add(MMOStatType.DEFENSE, 15.0D);
        }
    }

    private double baseHealth() {
        return club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getDouble("playerlevel.baseHp",
                club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getDouble("mmo.base-health",
                        club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getDouble("skills.base-mmo-health", 100.0D)));
    }

    private double baseMana() {
        return club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getDouble("playerlevel.baseMana",
                club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getDouble("mmo.base-mana",
                club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getDouble("custom-items.mana.max",
                        club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getDouble("actionbar.mana.max", 100.0D))));
    }

    private double baseDefense() {
        double configured = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getDouble("playerlevel.baseDefense",
                club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getDouble("mmo.base-defense",
                        club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getDouble("skills.base-mmo-defense", 0.0D)));
        return Math.max(0.0D, configured);
    }
}
