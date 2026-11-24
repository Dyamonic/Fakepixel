package me.outerspace.thepit.listeners;

import me.outerspace.thepit.ThePitPlugin;
import me.outerspace.thepit.managers.StatsManager;
import me.outerspace.thepit.managers.AchievementManager;
import org.bukkit.Material;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class StatisticsListener implements Listener {
    private final ThePitPlugin plugin;
    private final Set<UUID> inArena = new HashSet<>();

    public StatisticsListener(ThePitPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        Player attacker = null;
        boolean bow = false;
        if (event.getDamager() instanceof Player) attacker = (Player) event.getDamager();
        else if (event.getDamager() instanceof Arrow && ((Arrow) event.getDamager()).getShooter() instanceof Player) {
            attacker = (Player) ((Arrow) event.getDamager()).getShooter();
            bow = true;
        }
        if (attacker == null || victim.hasMetadata("BuildFFATestDamage") || attacker.hasMetadata("BuildFFATestDamage")
                || !plugin.getPerkManager().validCombat(attacker, victim)) return;
        double damage = Math.max(0.0D, event.getFinalDamage());
        if (damage <= 0.0D) return;
        StatsManager stats = plugin.getStatsManager();
        stats.addDoubleStat(attacker.getUniqueId(), StatsManager.DAMAGE_DEALT, damage);
        stats.addDoubleStat(victim.getUniqueId(), StatsManager.DAMAGE_TAKEN, damage);
        boolean attributedLava = plugin.getPerkListener().isAttributedLava(victim.getUniqueId());
        stats.addLongStat(attacker.getUniqueId(), StatsManager.LEFT_CLICKS, bow || attributedLava ? 0L : 1L);
        if (bow) {
            stats.addDoubleStat(attacker.getUniqueId(), StatsManager.BOW_DAMAGE_DEALT, damage);
            stats.addDoubleStat(victim.getUniqueId(), StatsManager.BOW_DAMAGE_TAKEN, damage);
        } else if (!attributedLava) {
            stats.addDoubleStat(attacker.getUniqueId(), StatsManager.MELEE_DAMAGE_DEALT, damage);
            stats.addDoubleStat(victim.getUniqueId(), StatsManager.MELEE_DAMAGE_TAKEN, damage);
            if (isSword(attacker.getItemInHand())) stats.addLongStat(attacker.getUniqueId(), StatsManager.SWORD_HITS, 1L);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBow(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();
        if (!plugin.getPerkManager().validParticipant(player) || !(event.getProjectile() instanceof Arrow)) return;
        plugin.getStatsManager().addLongStat(player.getUniqueId(), StatsManager.ARROWS_SHOT, 1L);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if ((event.getAction() == Action.LEFT_CLICK_BLOCK || event.getAction() == Action.LEFT_CLICK_AIR)
                && plugin.getArenaManager().isInCurrentPvpArena(event.getPlayer().getLocation())
                && !plugin.getArenaManager().isInCurrentSpawnProtection(event.getPlayer().getLocation())) {
            plugin.getStatsManager().addLongStat(event.getPlayer().getUniqueId(), StatsManager.LEFT_CLICKS, 1L);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (plugin.isBuildMode(event.getPlayer()) && !plugin.registry().settings().getBoolean("statistics.count-admin-build-mode", false)) return;
        if (!plugin.getArenaManager().isInCurrentPvpArena(event.getBlockPlaced().getLocation())) return;
        plugin.getStatsManager().addLongStat(event.getPlayer().getUniqueId(), StatsManager.BLOCKS_PLACED, 1L);
        if (plugin.getAchievementManager() != null) plugin.getAchievementManager().publish(
                new AchievementManager.ProgressEvent(event.getPlayer().getUniqueId(), AchievementManager.EventType.BLOCK_PLACED,
                        1L, java.util.Map.of("material", event.getBlockPlaced().getType().name()), false));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        if (plugin.isBuildMode(event.getPlayer()) && !plugin.registry().settings().getBoolean("statistics.count-admin-build-mode", false)) return;
        if (!plugin.getArenaManager().isInCurrentPvpArena(event.getBlock().getLocation())) return;
        plugin.getStatsManager().addLongStat(event.getPlayer().getUniqueId(), StatsManager.BLOCKS_BROKEN, 1L);
        boolean goldenPickaxe = plugin.getShopManager() != null && plugin.getShopManager().isItem(event.getPlayer().getItemInHand(), "golden-pickaxe");
        if (event.getBlock().getType() == Material.OBSIDIAN && !goldenPickaxe) plugin.getStatsManager().addLongStat(event.getPlayer().getUniqueId(), "obsidian_mined", 1L);
        if (plugin.getAchievementManager() != null) plugin.getAchievementManager().publish(
                new AchievementManager.ProgressEvent(event.getPlayer().getUniqueId(), AchievementManager.EventType.BLOCK_BROKEN,
                        1L, java.util.Map.of("material", event.getBlock().getType().name(), "golden_pickaxe", goldenPickaxe), false));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChat(AsyncPlayerChatEvent event) {
        if (!event.isCancelled()) plugin.getStatsManager().addLongStat(event.getPlayer().getUniqueId(), StatsManager.CHAT_MESSAGES, 1L);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onConsume(PlayerItemConsumeEvent event) {
        ItemStack item = event.getItem();
        if (item == null || item.getType() != Material.GOLDEN_APPLE) return;
        plugin.getStatsManager().addLongStat(event.getPlayer().getUniqueId(), StatsManager.GOLDEN_APPLES_EATEN, 1L);
        if (plugin.getContractManager() != null) plugin.getContractManager().onGoldenApple(event.getPlayer());
        if (plugin.getAchievementManager() != null) plugin.getAchievementManager().publish(
                new AchievementManager.ProgressEvent(event.getPlayer().getUniqueId(), AchievementManager.EventType.ITEM_CONSUMED,
                        1L, java.util.Map.of("id", "golden-apple"), false));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onLava(PlayerBucketEmptyEvent event) {
        if (event.getBucket() == Material.LAVA_BUCKET && plugin.getPerkManager().active(event.getPlayer(), "lava_bucket")) {
            plugin.getStatsManager().addLongStat(event.getPlayer().getUniqueId(), StatsManager.LAVA_BUCKETS_EMPTIED, 1L);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        if (event.getState() == PlayerFishEvent.State.FISHING
                && plugin.getArenaManager().isInCurrentPvpArena(event.getPlayer().getLocation())) {
            plugin.getStatsManager().addLongStat(event.getPlayer().getUniqueId(), StatsManager.FISHING_RODS_LAUNCHED, 1L);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onMove(PlayerMoveEvent event) {
        if (event.getTo() == null) return;
        UUID uuid = event.getPlayer().getUniqueId();
        boolean active = plugin.getArenaManager().isInCurrentPvpArena(event.getTo())
                && !plugin.getArenaManager().isInCurrentSpawnProtection(event.getTo());
        boolean previous = inArena.contains(uuid);
        if (active && !previous) {
            inArena.add(uuid);
            plugin.getStatsManager().addLongStat(uuid, StatsManager.JUMPS_INTO_PIT, 1L);
            if (plugin.getContractManager() != null) plugin.getContractManager().refreshActionBar(event.getPlayer());
        } else if (!active && previous) {
            inArena.remove(uuid);
            if (plugin.getArenaManager().isInCurrentSpawnProtection(event.getTo()) && plugin.getAchievementManager() != null) {
                plugin.getAchievementManager().publish(new AchievementManager.ProgressEvent(uuid,
                        AchievementManager.EventType.RETURNED_TO_SPAWN, 1L, java.util.Collections.emptyMap(), false));
            }
            if (plugin.getArenaManager().isInCurrentSpawnProtection(event.getTo()) && plugin.getContractManager() != null) {
                plugin.getContractManager().resetLife(event.getPlayer());
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player) || event.getInventory().getType() != InventoryType.ENDER_CHEST) return;
        if (plugin.getAchievementManager() != null) plugin.getAchievementManager().publish(
                new AchievementManager.ProgressEvent(player.getUniqueId(), AchievementManager.EventType.ENDER_CHEST_CHANGED,
                        1L, java.util.Collections.emptyMap(), false));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        inArena.remove(event.getPlayer().getUniqueId());
        plugin.getStatsManager().flushPlayer(event.getPlayer().getUniqueId(), true);
    }

    private boolean isSword(ItemStack item) {
        return item != null && item.getType().name().endsWith("_SWORD");
    }
}
