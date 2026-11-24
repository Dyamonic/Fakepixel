package club.abelix.games.fakevival.managers;

import club.abelix.games.fakevival.config.RuntimeSettings;
import club.abelix.games.fakevival.data.DataManager;
import club.abelix.games.fakevival.util.CC;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/** Lightweight, in-memory fishing skill check. Pending rewards are never persisted. */
public final class FishingMinigameManager implements Listener {
    public record PendingReward(ItemStack preview, String name, String rarity, String sizeClass, int xp,
                                Runnable grant) {}
    public record DebugState(boolean active, String rarity, double fishPosition, double barPosition,
                             double barSize, double progress, String reward, double hookDistance) {}

    private record Difficulty(double barSize, double fishSpeed, double gain, double loss, BarColor color) {}
    private static final Map<String, Difficulty> DIFFICULTIES = Map.of(
            "COMMON", new Difficulty(.24, .012, .018, .010, BarColor.GREEN),
            "UNCOMMON", new Difficulty(.22, .016, .016, .012, BarColor.GREEN),
            "RARE", new Difficulty(.19, .022, .014, .015, BarColor.YELLOW),
            "EPIC", new Difficulty(.17, .030, .012, .018, BarColor.PURPLE),
            "LEGENDARY", new Difficulty(.15, .040, .010, .022, BarColor.RED),
            "MYTHIC", new Difficulty(.13, .050, .009, .026, BarColor.RED));

    private final JavaPlugin plugin;
    private final DataManager data;
    private final Map<UUID, Session> sessions = new ConcurrentHashMap<>();
    private BukkitTask task;

    public FishingMinigameManager(JavaPlugin plugin, DataManager data) {
        this.plugin = plugin;
        this.data = data;
        startTask();
    }

    public boolean enabled() {
        String path = "runtime-settings.fishing.minigame.enabled";
        return data.getData().contains(path) ? data.getData().getBoolean(path)
                : RuntimeSettings.config(plugin).getBoolean("fishing.minigame.enabled", true);
    }

    public void setEnabled(boolean enabled) {
        data.getData().set("runtime-settings.fishing.minigame.enabled", enabled);
        data.save();
        if (!enabled) sessions.keySet().forEach(id -> finish(id, false, "&cThe fish got away!"));
    }

    public boolean isActive(UUID player) { return sessions.containsKey(player); }

    public boolean start(Player player, PendingReward reward, Location hookLocation, ItemStack rod) {
        if (!enabled() || player == null || reward == null || hookLocation == null || sessions.containsKey(player.getUniqueId())) return false;
        String rarity = normalizeRarity(reward.rarity());
        Difficulty difficulty = DIFFICULTIES.getOrDefault(rarity, DIFFICULTIES.get("COMMON"));
        double sizeAdjust = switch (normalize(reward.sizeClass())) {
            case "tiny" -> .035; case "small" -> .02; case "huge" -> -.015; case "giant" -> -.03; default -> 0;
        };
        club.abelix.games.fakevival.mmo.MMOStatMap rodStats = new club.abelix.games.fakevival.mmo.MMOStatStorage(plugin).read(rod);
        double barBonus = (pdcDouble(rod, "fishing_bar_size_bonus") + rodStats.get(club.abelix.games.fakevival.mmo.MMOStatType.FISHING_BAR_SIZE)) / 100.0D;
        double progressBonus = (pdcDouble(rod, "fishing_progress_bonus") + rodStats.get(club.abelix.games.fakevival.mmo.MMOStatType.FISHING_PROGRESS_BONUS)) / 100.0D;
        double stability = Math.clamp(pdcDouble(rod, "fishing_stability") / 100.0D, 0, .75);
        double resistance = Math.clamp(pdcDouble(rod, "fish_escape_resistance") / 100.0D, 0, .75);
        CustomEnchantManager customEnchants = plugin instanceof club.abelix.games.fakevival.Game game ? game.getCustomEnchantManager() : null;
        int steadyLine = customEnchants == null ? 0 : customEnchants.effectLevel(rod, "steady_line");
        int quickReel = customEnchants == null ? 0 : customEnchants.effectLevel(rod, "quick_reel");
        int calmCast = customEnchants == null ? 0 : customEnchants.effectLevel(rod, "calm_cast");
        Difficulty adjusted = new Difficulty(difficulty.barSize(), difficulty.fishSpeed() * Math.max(0.70D, 1.0D - calmCast * 0.04D),
                difficulty.gain(), difficulty.loss(), difficulty.color());
        BossBar boss = Bukkit.createBossBar("", difficulty.color(), BarStyle.SOLID);
        boss.addPlayer(player); boss.setVisible(true); boss.setProgress(.35);
        Session session = new Session(player.getUniqueId(), reward, hookLocation.clone(), player.getWorld().getUID(), boss,
                adjusted, Math.clamp(difficulty.barSize() + sizeAdjust + barBonus + steadyLine * 0.02D, .08, .42),
                difficulty.gain() * (1.0 + progressBonus + quickReel * 0.02D), difficulty.loss() * (1.0 - resistance), stability);
        sessions.put(player.getUniqueId(), session);
        player.sendMessage(CC.color("&eHold &fSneak &eto raise the fishing bar."));
        player.sendMessage(CC.color("&7Keep the fish inside the green bar!"));
        actionbar(player, "&eHold Sneak to raise the bar! &7Keep &b🐟 &7inside &a|||||");
        render(player, session);
        return true;
    }

    public void startTest(Player player, String rarity) {
        ItemStack preview = new ItemStack(Material.COD);
        String normalized = normalizeRarity(rarity);
        start(player, new PendingReward(preview, pretty(normalized) + " Test Fish", normalized, "NORMAL", 0,
                () -> player.sendMessage(CC.color("&aFishing minigame test completed."))), player.getLocation(), player.getInventory().getItemInMainHand());
    }

    public DebugState debug(Player player) {
        Session s = player == null ? null : sessions.get(player.getUniqueId());
        if (s == null) return new DebugState(false, "NONE", 0, 0, 0, 0, "None", 0);
        return new DebugState(true, normalizeRarity(s.reward.rarity()), s.fishPosition, s.barPosition, s.barSize,
                s.progress, s.reward.name(), distance(player, s));
    }

    public void shutdown() {
        if (task != null) task.cancel();
        task = null;
        sessions.keySet().forEach(id -> finish(id, false, null));
    }

    private void startTask() {
        long interval = Math.max(1L, RuntimeSettings.config(plugin).getLong("fishing.minigame.tickInterval", 2L));
        task = Bukkit.getScheduler().runTaskTimer(plugin, () -> tick(interval), interval, interval);
    }

    private void tick(long elapsedTicks) {
        if (!enabled()) { sessions.keySet().forEach(id -> finish(id, false, null)); return; }
        for (Session s : sessions.values().toArray(Session[]::new)) {
            Player player = Bukkit.getPlayer(s.playerId);
            if (player == null || !player.isOnline()) { finish(s.playerId, false, null); continue; }
            if (!player.getWorld().getUID().equals(s.worldId)) { finish(s.playerId, false, "&cThe fish got away!"); continue; }
            if (distance(player, s) > RuntimeSettings.config(plugin).getDouble("fishing.minigame.maxDistanceFromHook", 12D)) {
                finish(s.playerId, false, "&cThe fish got away! &7You moved too far from the hook."); continue;
            }
            s.elapsedTicks += elapsedTicks;
            boolean rodHeld = player.getInventory().getItemInMainHand().getType() == Material.FISHING_ROD
                    || player.getInventory().getItemInOffHand().getType() == Material.FISHING_ROD;
            s.rodAwayTicks = rodHeld ? 0 : s.rodAwayTicks + elapsedTicks;
            if (s.rodAwayTicks > 40) { finish(s.playerId, false, "&cThe fish got away! &7Keep your fishing rod equipped."); continue; }

            boolean raising = player.isSneaking() || System.currentTimeMillis() < s.pulseUntil;
            s.barVelocity += raising ? .035 : -.025;
            s.barVelocity *= .85 * (1.0 - s.stability * .25);
            s.barPosition += s.barVelocity;
            if (s.barPosition <= 0) { s.barPosition = 0; s.barVelocity = 0; }
            if (s.barPosition >= 1) { s.barPosition = 1; s.barVelocity = 0; }

            if (--s.directionTicks <= 0) {
                s.directionTicks = ThreadLocalRandom.current().nextInt(3, Math.max(4, 12 - difficultyIndex(s.reward.rarity())));
                s.fishTargetVelocity = ThreadLocalRandom.current().nextBoolean() ? s.difficulty.fishSpeed() : -s.difficulty.fishSpeed();
                if (ThreadLocalRandom.current().nextDouble() < .2) s.fishTargetVelocity *= 1.6;
            }
            s.fishVelocity += (s.fishTargetVelocity - s.fishVelocity) * (.28 + difficultyIndex(s.reward.rarity()) * .035);
            s.fishPosition += s.fishVelocity;
            if (s.fishPosition <= 0 || s.fishPosition >= 1) {
                s.fishPosition = Math.clamp(s.fishPosition, 0, 1); s.fishVelocity *= -.65; s.fishTargetVelocity *= -1;
            }

            boolean inside = s.fishPosition >= s.barPosition - s.barSize / 2 && s.fishPosition <= s.barPosition + s.barSize / 2;
            s.progress = Math.clamp(s.progress + (inside ? s.progressGain : -s.progressLoss), 0, 1);
            render(player, s);
            if (s.progress >= 1) finish(s.playerId, true, null);
            else if (s.progress <= 0 && s.elapsedTicks >= RuntimeSettings.config(plugin).getLong("fishing.minigame.startGraceTicks", 60L))
                finish(s.playerId, false, "&cThe fish got away!");
        }
    }

    private void render(Player player, Session s) {
        int segments = 21;
        int fish = Math.clamp((int) Math.round(s.fishPosition * (segments - 1)), 0, segments - 1);
        int low = Math.clamp((int) Math.floor((s.barPosition - s.barSize / 2) * segments), 0, segments - 1);
        int high = Math.clamp((int) Math.ceil((s.barPosition + s.barSize / 2) * segments), 0, segments - 1);
        StringBuilder bar = new StringBuilder("&8[");
        for (int i = 0; i < segments; i++) {
            if (i == fish) bar.append("&e🐟");
            else if (i >= low && i <= high) bar.append("&a|");
            else bar.append("&7-");
        }
        bar.append("&8] &b").append((int) Math.round(s.progress * 100)).append('%');
        String rendered = CC.color(bar.toString());
        s.bossBar.setTitle(rendered); s.bossBar.setProgress(Math.clamp(s.progress, 0, 1));
        if (RuntimeSettings.config(plugin).getString("fishing.minigame.visual", "BOSSBAR").equalsIgnoreCase("ACTIONBAR")) actionbar(player, bar.toString());
    }

    private void finish(UUID id, boolean won, String message) {
        Session s = sessions.remove(id);
        if (s == null || s.finished) return;
        s.finished = true; s.bossBar.removeAll(); s.bossBar.setVisible(false);
        Player player = Bukkit.getPlayer(id);
        if (won) {
            try { s.reward.grant().run(); }
            catch (Throwable error) { plugin.getLogger().warning("[SMP Fishing] Could not grant completed minigame reward: " + error.getMessage()); return; }
            if (player != null) player.sendMessage(CC.color("&aYou caught &f" + s.reward.name() + "&a!"));
        } else if (player != null && message != null) player.sendMessage(CC.color(message));
    }

    @EventHandler public void onQuit(PlayerQuitEvent event) { finish(event.getPlayer().getUniqueId(), false, null); }
    @EventHandler public void onDeath(PlayerDeathEvent event) {
        if (RuntimeSettings.config(plugin).getBoolean("fishing.minigame.failOnDeath", true)) finish(event.getEntity().getUniqueId(), false, "&cThe fish got away!");
    }
    @EventHandler public void onWorldChange(PlayerChangedWorldEvent event) {
        if (RuntimeSettings.config(plugin).getBoolean("fishing.minigame.failOnWorldChange", true)) finish(event.getPlayer().getUniqueId(), false, "&cThe fish got away!");
    }
    @EventHandler public void onInteract(PlayerInteractEvent event) {
        if (!event.getAction().isRightClick() || event.getItem() == null || event.getItem().getType() != Material.FISHING_ROD) return;
        Session s = sessions.get(event.getPlayer().getUniqueId());
        if (s != null) { event.setCancelled(true); s.pulseUntil = System.currentTimeMillis() + 250L; }
    }

    private double distance(Player player, Session session) {
        return player.getWorld().getUID().equals(session.worldId) ? player.getLocation().distance(session.hookLocation) : Double.POSITIVE_INFINITY;
    }
    private double pdcDouble(ItemStack item, String key) {
        if (item == null || !item.hasItemMeta()) return 0;
        return item.getItemMeta().getPersistentDataContainer().getOrDefault(new NamespacedKey(plugin, key), PersistentDataType.DOUBLE, 0D);
    }
    private static int difficultyIndex(String rarity) { return switch (normalizeRarity(rarity)) { case "UNCOMMON" -> 1; case "RARE" -> 2; case "EPIC" -> 3; case "LEGENDARY" -> 4; case "MYTHIC" -> 5; default -> 0; }; }
    private static String normalizeRarity(String rarity) { String v = rarity == null ? "COMMON" : rarity.toUpperCase(Locale.ROOT); return DIFFICULTIES.containsKey(v) ? v : "COMMON"; }
    private static String normalize(String value) { return value == null ? "" : value.toLowerCase(Locale.ROOT); }
    private static String pretty(String value) { String lower = normalize(value); return lower.isEmpty() ? "Fish" : Character.toUpperCase(lower.charAt(0)) + lower.substring(1); }
    private static void actionbar(Player player, String message) { player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(CC.color(message))); }

    private static final class Session {
        final UUID playerId; final PendingReward reward; final Location hookLocation; final UUID worldId; final BossBar bossBar;
        final Difficulty difficulty; final double barSize; final double progressGain; final double progressLoss; final double stability;
        double fishPosition = .5, fishVelocity, fishTargetVelocity = .01, barPosition = .5, barVelocity, progress = .35;
        long elapsedTicks, rodAwayTicks, pulseUntil; int directionTicks = 5; boolean finished;
        Session(UUID playerId, PendingReward reward, Location hookLocation, UUID worldId, BossBar bossBar,
                Difficulty difficulty, double barSize, double progressGain, double progressLoss, double stability) {
            this.playerId = playerId; this.reward = reward; this.hookLocation = hookLocation; this.worldId = worldId; this.bossBar = bossBar;
            this.difficulty = difficulty; this.barSize = barSize; this.progressGain = progressGain; this.progressLoss = progressLoss; this.stability = stability;
        }
    }
}
