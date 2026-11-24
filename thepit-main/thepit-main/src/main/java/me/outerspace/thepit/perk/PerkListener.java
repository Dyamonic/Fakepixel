package me.outerspace.thepit.perk;

import me.outerspace.thepit.ThePitPlugin;
import me.outerspace.thepit.registry.RegistryNode;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class PerkListener implements Listener {
    private final java.util.Set<java.util.UUID> goldenHeadConsumeLocks = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private record Hit(double damage, long time) {}
    private final ThePitPlugin plugin;
    private final PerkManager manager;
    private final Map<UUID, Map<UUID, Hit>> assists = new HashMap<>();
    private final Set<UUID> bonkUsed = new HashSet<>();
    private final Map<UUID, Integer> strengthStacks = new HashMap<>();
    private final Map<UUID, Long> strengthUntil = new HashMap<>();
    private final Map<UUID, Map<UUID, Long>> firstHits = new HashMap<>();
    private final Map<UUID, Map<UUID, Integer>> fistHits = new HashMap<>();
    private final Map<UUID, Integer> reconHits = new HashMap<>();
    private final Map<UUID, Long> reconLastHit = new HashMap<>();
    private final Map<UUID, Map<UUID, Long>> fistLastHit = new HashMap<>();
    private final Set<UUID> soupBonus = new HashSet<>();
    private final Map<String, UUID> lavaOwners = new HashMap<>();
    private final Set<UUID> lavaDamage = new HashSet<>();

    public PerkListener(ThePitPlugin plugin, PerkManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        Player attacker = attacker(event.getDamager());
        if (attacker == null || !manager.validCombat(attacker, victim)) return;
        if (manager.has(victim, "safety_first")) Bukkit.getScheduler().runTask(plugin, () -> manager.restoreProtectedArmor(victim));
        double damage = event.getDamage();
        if (plugin.getShopManager() != null && event.getDamager() instanceof Player) damage = plugin.getShopManager().baseDamage(attacker, victim, damage);
        if (manager.has(victim, "bonk") && !bonkUsed.contains(victim.getUniqueId())) {
            bonkUsed.add(victim.getUniqueId());
            event.setCancelled(true);
            manager.effect(victim, "bonk", "mechanics.resistance");
            manager.play(victim, "bonk", "mechanics.sound");
            return;
        }
        if (manager.has(attacker, "strength_chaining")) {
            long now = System.currentTimeMillis();
            int stacks = strengthUntil.getOrDefault(attacker.getUniqueId(), 0L) > now ? strengthStacks.getOrDefault(attacker.getUniqueId(), 0) : 0;
            damage *= 1.0D + stacks * manager.value("strength_chaining").getDouble("mechanics.damage-per-stack", 0.08D);
        }
        if (manager.has(attacker, "bounty_hunter") && bountyArmorActive(attacker)) {
            RegistryNode settings = manager.value("bounty_hunter");
            double per = settings.getDouble("mechanics.damage-per-bounty-unit", 0.01D);
            int unit = Math.max(1, settings.getInt("mechanics.bounty-unit", 100));
            double maximum = settings.getDouble("mechanics.maximum-damage-bonus", 0.50D);
            damage *= 1.0D + Math.min(maximum, (plugin.getStatsManager().getBounty(victim.getUniqueId()) / (double) unit) * per);
        }
        if (manager.has(victim, "gladiator")) {
            RegistryNode settings = manager.value("gladiator");
            double radius = settings.getDouble("mechanics.radius", 12.0D);
            int nearby = 0;
            for (Player player : victim.getWorld().getPlayers()) if (player != victim && manager.validParticipant(player) && player.getLocation().distanceSquared(victim.getLocation()) <= radius * radius) nearby++;
            if (nearby >= settings.getInt("mechanics.minimum-nearby-players", 3)) {
                double reduction = Math.min(settings.getDouble("mechanics.maximum-reduction", 0.30D), nearby * settings.getDouble("mechanics.reduction-per-player", 0.03D));
                damage *= Math.max(0.0D, 1.0D - reduction);
            }
        }
        if (manager.has(attacker, "marathon") && attacker.hasPotionEffect(PotionEffectType.SPEED)) damage *= 1.0D + manager.value("marathon").getDouble("mechanics.outgoing-damage-bonus", 0.18D);
        if (manager.has(victim, "marathon") && victim.hasPotionEffect(PotionEffectType.SPEED)) damage *= 1.0D - manager.value("marathon").getDouble("mechanics.incoming-damage-reduction", 0.18D);
        if (manager.has(attacker, "first_strike") && firstStrike(attacker, victim)) {
            damage *= 1.0D + manager.value("first_strike").getDouble("mechanics.first-hit-damage-bonus", 0.35D);
            manager.effect(attacker, "first_strike", "mechanics.speed");
        }
        if (manager.has(attacker, "recon") && event.getDamager() instanceof Arrow) {
            long now=System.currentTimeMillis();long reset=Math.max(1,manager.value("recon").getLong("mechanics.hit-reset-seconds",30))*1000L;
            if(now-reconLastHit.getOrDefault(attacker.getUniqueId(),0L)>reset)reconHits.remove(attacker.getUniqueId());
            reconLastHit.put(attacker.getUniqueId(),now);
            int hits = reconHits.merge(attacker.getUniqueId(), 1, Integer::sum);
            int every = Math.max(1, manager.value("recon").getInt("mechanics.hits-required", 4));
            if (hits % every == 0) {
                damage *= 1.0D + manager.value("recon").getDouble("mechanics.bonus-damage", 0.50D);
                int reward = manager.value("recon").getInt("mechanics.xp-reward", 4);
                Bukkit.getScheduler().runTask(plugin, () -> plugin.getStatsManager().addXp(attacker, reward, me.outerspace.thepit.managers.StatsManager.XpSource.EVENT));
            }
        }
        if (manager.has(attacker, "kung_fu_knowledge")) {
            ItemStack held = attacker.getItemInHand();
            if (held != null && held.getType() != Material.AIR) {
                event.setCancelled(true);
                return;
            }
            RegistryNode settings = manager.value("kung_fu_knowledge");
            damage = settings.getDouble("mechanics.fist-damage", 7.4D);
            long now=System.currentTimeMillis();long reset=Math.max(1,settings.getLong("mechanics.target-reset-seconds",15))*1000L;
            Map<UUID,Long> last=fistLastHit.computeIfAbsent(attacker.getUniqueId(),ignored->new HashMap<>());
            if(now-last.getOrDefault(victim.getUniqueId(),0L)>reset)fistHits.computeIfAbsent(attacker.getUniqueId(),ignored->new HashMap<>()).remove(victim.getUniqueId());
            last.put(victim.getUniqueId(),now);
            int hits = fistHits.computeIfAbsent(attacker.getUniqueId(), ignored -> new HashMap<>()).merge(victim.getUniqueId(), 1, Integer::sum);
            if (hits % Math.max(1, settings.getInt("mechanics.hits-for-speed", 4)) == 0) manager.effect(attacker, "kung_fu_knowledge", "mechanics.speed");
        }
        if (manager.has(attacker, "barbarian") && isAxe(attacker.getItemInHand())) damage += manager.value("barbarian").getDouble("mechanics.axe-bonus-base-damage", 1.0D);
        if (soupBonus.remove(attacker.getUniqueId()) && !(event.getDamager() instanceof Arrow)) damage *= 1.0D + manager.value("soup").getDouble("mechanics.next-hit-damage-bonus", 0.15D);
        if (plugin.getPassiveManager() != null) {
            if (event.getDamager() instanceof Arrow) {
                damage *= 1.0D + plugin.getPassiveManager().bonus(attacker.getUniqueId(), "bow-damage");
                me.outerspace.thepit.perk.PassiveDefinition reduction = plugin.getPassiveManager().definition("damage-reduction");
                if (reduction != null && ("BOW_ONLY".equalsIgnoreCase(reduction.affectedDamage()) || "ALL".equalsIgnoreCase(reduction.affectedDamage()))) {
                    damage *= Math.max(0.0D, 1.0D - plugin.getPassiveManager().bonus(victim.getUniqueId(), "damage-reduction"));
                }
            } else if (event.getDamager() instanceof Player && !lavaDamage.contains(victim.getUniqueId())) {
                damage *= 1.0D + plugin.getPassiveManager().bonus(attacker.getUniqueId(), "melee-damage");
                me.outerspace.thepit.perk.PassiveDefinition reduction = plugin.getPassiveManager().definition("damage-reduction");
                if (reduction != null && ("ALL".equalsIgnoreCase(reduction.affectedDamage()) || "MELEE_ONLY".equalsIgnoreCase(reduction.affectedDamage()))) {
                    damage *= Math.max(0.0D, 1.0D - plugin.getPassiveManager().bonus(victim.getUniqueId(), "damage-reduction"));
                }
            }
        }
        if (plugin.getShopManager() != null) damage = plugin.getShopManager().finalDamage(attacker, victim, damage);
        if (plugin.getKillstreakManager() != null) damage = plugin.getKillstreakManager().modifyDamage(attacker, victim, damage, event.getDamager() instanceof Arrow);
        event.setDamage(Math.max(0.0D, damage));
        if (plugin.getKillstreakManager() != null) plugin.getKillstreakManager().applyPostDamage(attacker, victim);
        if (manager.has(attacker, "vampire")) {
            double healing = event.getDamager() instanceof Arrow && ((Arrow) event.getDamager()).getVelocity().lengthSquared() >= manager.value("vampire").getDouble("mechanics.full-bow-velocity-squared", 8.0D)
                    ? manager.value("vampire").getDouble("mechanics.full-bow-healing", 3.0D)
                    : manager.value("vampire").getDouble("mechanics.melee-healing", 1.0D);
            double before = attacker.getHealth();
            manager.heal(attacker, healing);
            double restored = Math.max(0.0D, attacker.getHealth() - before);
            if (restored > 0.0D) {
                plugin.getStatsManager().addDoubleStat(attacker.getUniqueId(), "vampire_health_restored", restored);
                achievementScore(attacker, "vampire_health_restored", (long) Math.floor(plugin.getStatsManager().getDoubleStat(attacker.getUniqueId(), "vampire_health_restored")));
            }
        }
        if (manager.has(attacker, "spammer") && event.getDamager() instanceof Arrow) giveArrows(attacker);
        assists.computeIfAbsent(victim.getUniqueId(), ignored -> new LinkedHashMap<>()).put(attacker.getUniqueId(), new Hit(event.getFinalDamage(), System.currentTimeMillis()));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEnvironmentalDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player victim) || lavaDamage.contains(victim.getUniqueId())) return;
        if (event.getCause() != EntityDamageEvent.DamageCause.LAVA && event.getCause() != EntityDamageEvent.DamageCause.FIRE_TICK && event.getCause() != EntityDamageEvent.DamageCause.FIRE) return;
        UUID ownerId = nearbyLavaOwner(victim.getLocation());
        Player owner = ownerId == null ? null : Bukkit.getPlayer(ownerId);
        if (owner == null || !manager.validCombat(owner, victim)) return;
        if (plugin.getContractManager() != null) plugin.getContractManager().recordLavaAttribution(victim, owner, victim.getLocation());
        event.setCancelled(true);
        lavaDamage.add(victim.getUniqueId());
        try { victim.damage(event.getDamage(), owner); }
        finally { lavaDamage.remove(victim.getUniqueId()); }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDeathRewards(PlayerDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer != null && killer != event.getEntity()) manager.onLuckyDeath(killer, event.getEntity());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();
        if (killer != null && killer != victim) {
            applyKillStacks(killer);
            Bukkit.getScheduler().runTask(plugin, () -> manager.onKill(killer, victim));
        }
        Map<UUID, Hit> contributors = assists.remove(victim.getUniqueId());
        if (contributors != null) {
            long cutoff = System.currentTimeMillis() - plugin.registry().perks().getLong("assists.combat-timeout-seconds", 15) * 1000L;
            double total = 0.0D;
            for (Map.Entry<UUID, Hit> entry : contributors.entrySet()) if (entry.getValue().time() >= cutoff) total += entry.getValue().damage();
            double finalTotal = total;
            Bukkit.getScheduler().runTask(plugin, () -> {
                for (Map.Entry<UUID, Hit> entry : contributors.entrySet()) {
                    if (entry.getValue().time() < cutoff || killer != null && entry.getKey().equals(killer.getUniqueId())) continue;
                    Player assistant = Bukkit.getPlayer(entry.getKey());
                    if (assistant != null) manager.onAssist(assistant, finalTotal <= 0.0D ? 0.0D : entry.getValue().damage() / finalTotal);
                }
            });
        }
        clearLife(victim.getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        manager.load(event.getPlayer().getUniqueId());
        manager.revalidate(event.getPlayer());
        manager.applyHealth(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        clearAll(uuid);
        manager.unload(uuid);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(PlayerRespawnEvent event) {
        clearLife(event.getPlayer().getUniqueId());
        Bukkit.getScheduler().runTask(plugin, () -> manager.applyHealth(event.getPlayer()));
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) { clearCombat(event.getPlayer().getUniqueId());manager.applyHealth(event.getPlayer()); }

    @EventHandler(ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        if (event.getFrom().getWorld() != event.getTo().getWorld()) clearCombat(event.getPlayer().getUniqueId());
    }

    @EventHandler(ignoreCancelled = true)
    public void onLavaPlace(PlayerBucketEmptyEvent event) {
        if (event.getBucket() != Material.LAVA_BUCKET || !manager.active(event.getPlayer(), "lava_bucket")) return;
        Block block = event.getBlockClicked().getRelative(event.getBlockFace());
        String key = blockKey(block);
        lavaOwners.put(key, event.getPlayer().getUniqueId());
        long ticks = Math.max(20L, manager.value("lava_bucket").getLong("mechanics.owner-duration-seconds", 60) * 20L);
        Bukkit.getScheduler().runTaskLater(plugin, () -> lavaOwners.remove(key), ticks);
    }

    @EventHandler(ignoreCancelled = true)
    public void onLavaPickup(PlayerBucketFillEvent event) {
        if (event.getBlockClicked().getType() == Material.LAVA || event.getBlockClicked().getType() == Material.STATIONARY_LAVA) {
            lavaOwners.remove(blockKey(event.getBlockClicked()));
            if (plugin.getContractManager() != null) plugin.getContractManager().clearLavaOwner(event.getPlayer().getUniqueId());
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        ItemStack item = event.getItem();
        if (manager.special(item, "golden_head")) {
            event.setCancelled(true);
            consumeGoldenHead(event.getPlayer(), item);
        } else if (manager.special(item, "olympus")) {
            event.setCancelled(true);
            consume(event.getPlayer(), item);
            consumed(event.getPlayer(), "olympus-potion");
            manager.effect(event.getPlayer(), "olympus", "mechanics.speed");
            manager.effect(event.getPlayer(), "olympus", "mechanics.regeneration");
            manager.effect(event.getPlayer(), "olympus", "mechanics.resistance");
            int coins = manager.value("olympus").getInt("mechanics.coin-reward", 0);
            int xp = manager.value("olympus").getInt("mechanics.xp-reward", 27);
            Bukkit.getScheduler().runTask(plugin, () -> { plugin.getPitGoldService().award(event.getPlayer(), coins, ThePitPlugin.GoldSource.EVENT);plugin.getStatsManager().addXp(event.getPlayer(), xp, me.outerspace.thepit.managers.StatsManager.XpSource.EVENT); });
        } else if (manager.special(item, "kill_soup") || manager.special(item, "assist_soup")) {
            event.setCancelled(true);
            boolean kill = manager.special(item, "kill_soup");
            consume(event.getPlayer(), item);
            consumed(event.getPlayer(), "tasty-soup");
            String path = kill ? "mechanics.kill-soup" : "mechanics.assist-soup";
            soupEffects(event.getPlayer(), path);
            soupBonus.add(event.getPlayer().getUniqueId());
        }
    }


    private void consumeGoldenHead(Player player, ItemStack item) {
        if (player == null || item == null || player.isDead() || player.getHealth() <= 0.0D) return;
        UUID uuid = player.getUniqueId();
        if (!goldenHeadConsumeLocks.add(uuid)) return;
        try {
            if (!manager.special(player.getItemInHand(), "golden_head")) return;
            ItemStack before = player.getItemInHand().clone();
            consume(player, player.getItemInHand());
            try {
                manager.effect(player, "golden_heads", "mechanics.speed");
                manager.effect(player, "golden_heads", "mechanics.regeneration");
                absorption(player, "golden_heads", "mechanics.absorption");
                plugin.getStatsManager().addLongStat(uuid, me.outerspace.thepit.managers.StatsManager.GOLDEN_HEADS_EATEN, 1L);
                consumed(player, "golden-head");
                if (plugin.getProgressManager() != null) plugin.getProgressManager().recordProgress(player,
                        me.outerspace.thepit.managers.ProgressManager.ObjectiveType.GOLDEN_HEAD_CONSUMED,
                        "golden_head", 1L,
                        new me.outerspace.thepit.managers.ProgressManager.ProgressContext("golden_head",
                                "golden-head:" + uuid + ":" + System.currentTimeMillis() / 50L,
                                java.util.Collections.emptyMap()));
            } catch (Throwable failure) {
                player.setItemInHand(before);
                player.updateInventory();
                plugin.getLogger().warning("Golden Head effects failed for " + uuid + ": " + failure.getMessage());
            }
        } finally {
            Bukkit.getScheduler().runTaskLater(plugin, () -> goldenHeadConsumeLocks.remove(uuid), 2L);
        }
    }
    private void consumed(Player player, String id) {
        if (plugin.getAchievementManager() != null) plugin.getAchievementManager().publish(
                new me.outerspace.thepit.managers.AchievementManager.ProgressEvent(player.getUniqueId(),
                        me.outerspace.thepit.managers.AchievementManager.EventType.ITEM_CONSUMED,
                        1L, java.util.Map.of("id", id), false));
    }

    @EventHandler(ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        if (manager.protectedItem(event.getItemDrop().getItemStack())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventory(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (manager.protectedItem(event.getCurrentItem()) || manager.protectedItem(event.getCursor())) {
            event.setCancelled(true);
            player.updateInventory();
            return;
        }
        if (manager.has(player, "marathon") && (isBoots(event.getCurrentItem()) || isBoots(event.getCursor()))) {
            event.setCancelled(true);
            player.updateInventory();
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (manager.protectedItem(event.getOldCursor()) || manager.has(player, "marathon") && event.getRawSlots().contains(36)) {
            event.setCancelled(true);
            player.updateInventory();
        }
    }

    public void cleanupArena() {
        assists.clear();
        bonkUsed.clear();
        strengthStacks.clear();
        strengthUntil.clear();
        firstHits.clear();
        fistHits.clear();
        reconHits.clear();
        reconLastHit.clear();
        fistLastHit.clear();
        soupBonus.clear();
        lavaOwners.clear();
        lavaDamage.clear();
        for (Player player : Bukkit.getOnlinePlayers()) manager.applyHealth(player);
    }

    public boolean isAttributedLava(UUID uuid) {
        return lavaDamage.contains(uuid);
    }

    public boolean isMaximumStrength(Player player) {
        if (player == null || !manager.has(player, "strength_chaining")
                || strengthUntil.getOrDefault(player.getUniqueId(), 0L) <= System.currentTimeMillis()) return false;
        int maximum = Math.max(1, manager.value("strength_chaining").getInt("mechanics.maximum-stacks", 5));
        return strengthStacks.getOrDefault(player.getUniqueId(), 0) >= maximum;
    }

    private Player attacker(Entity entity) {
        if (entity instanceof Player player) return player;
        if (entity instanceof Arrow arrow && arrow.getShooter() instanceof Player player) return player;
        return null;
    }
    private void applyKillStacks(Player killer) {
        if (!manager.has(killer, "strength_chaining")) return;
        RegistryNode settings = manager.value("strength_chaining");
        int maximum = Math.max(1, settings.getInt("mechanics.maximum-stacks", 5));
        strengthStacks.put(killer.getUniqueId(), Math.min(maximum, strengthStacks.getOrDefault(killer.getUniqueId(), 0) + 1));
        strengthUntil.put(killer.getUniqueId(), System.currentTimeMillis() + Math.max(1, settings.getLong("mechanics.duration-seconds", 7)) * 1000L);
    }
    private boolean firstStrike(Player attacker, Player victim) {
        long now = System.currentTimeMillis();
        long timeout = Math.max(1, manager.value("first_strike").getLong("mechanics.combat-reset-seconds", 15)) * 1000L;
        Map<UUID, Long> targets = firstHits.computeIfAbsent(attacker.getUniqueId(), ignored -> new HashMap<>());
        long previous = targets.getOrDefault(victim.getUniqueId(), 0L);
        targets.put(victim.getUniqueId(), now);
        return now - previous > timeout;
    }
    private boolean bountyArmorActive(Player player) {
        if (!manager.value("bounty_hunter").getBoolean("mechanics.require-armor", true)) return true;
        return manager.special(player.getInventory().getLeggings(), "bounty_armor");
    }
    private boolean isAxe(ItemStack item) { return item != null && item.getType().name().endsWith("_AXE"); }
    private boolean isBoots(ItemStack item) { return item != null && item.getType().name().endsWith("_BOOTS"); }
    private void giveArrows(Player player) {
        int amount = manager.value("spammer").getInt("mechanics.arrows-per-hit", 3);
        int limit = manager.value("spammer").getInt("mechanics.arrow-limit", 64);
        int current = 0;
        for (ItemStack item : player.getInventory().getContents()) if (item != null && item.getType() == Material.ARROW) current += item.getAmount();
        int add = Math.min(amount, Math.max(0, limit - current));
        if (add > 0) {
            player.getInventory().addItem(new ItemStack(Material.ARROW, add));
            plugin.getStatsManager().addLongStat(player.getUniqueId(), "spammer_arrows_received", add);
            achievementScore(player, "spammer_arrows_received", plugin.getStatsManager().getLongStat(player.getUniqueId(), "spammer_arrows_received"));
        }
    }
    private void achievementScore(Player player, String id, long value) {
        if (plugin.getAchievementManager() != null) plugin.getAchievementManager().publish(
                new me.outerspace.thepit.managers.AchievementManager.ProgressEvent(player.getUniqueId(),
                        me.outerspace.thepit.managers.AchievementManager.EventType.EVENT_SCORE_CHANGED,
                        value, java.util.Map.of("id", id), false));
    }
    private void absorption(Player player,String perk,String path) {
        RegistryNode settings=manager.value(perk);PotionEffectType type=PotionEffectType.ABSORPTION;
        int duration=Math.max(1,settings.getInt(path+".duration-seconds",120))*20;int level=Math.max(1,settings.getInt(path+".level",1))-1;
        player.addPotionEffect(new PotionEffect(type,duration,level),true);
    }
    private void soupEffects(Player player,String path) {
        RegistryNode settings=manager.value("soup");manager.heal(player,settings.getDouble(path+".healing",0.0D));
        for(String effectName:new String[]{"speed","regeneration","absorption"}){
            String base=path+"."+effectName;PotionEffectType type=PotionEffectType.getByName(settings.getString(base+".type",effectName).toUpperCase());
            if(type!=null)player.addPotionEffect(new PotionEffect(type,Math.max(1,settings.getInt(base+".duration-seconds",5))*20,Math.max(0,settings.getInt(base+".level",1)-1)),true);
        }
    }
    private void consume(Player player,ItemStack item){if(item.getAmount()<=1)player.setItemInHand(null);else item.setAmount(item.getAmount()-1);player.updateInventory();}
    private void clearLife(UUID uuid){bonkUsed.remove(uuid);strengthStacks.remove(uuid);strengthUntil.remove(uuid);soupBonus.remove(uuid);clearCombat(uuid);}
    private void clearCombat(UUID uuid){firstHits.remove(uuid);fistHits.remove(uuid);fistLastHit.remove(uuid);reconHits.remove(uuid);reconLastHit.remove(uuid);for(Map<UUID,Long> values:firstHits.values())values.remove(uuid);for(Map<UUID,Integer> values:fistHits.values())values.remove(uuid);for(Map<UUID,Long> values:fistLastHit.values())values.remove(uuid);for(Map<UUID,Hit> values:assists.values())values.remove(uuid);assists.remove(uuid);}
    private void clearAll(UUID uuid){clearLife(uuid);}
    private String blockKey(Block block){Location location=block.getLocation();return location.getWorld().getName()+":"+location.getBlockX()+":"+location.getBlockY()+":"+location.getBlockZ();}
    private UUID nearbyLavaOwner(Location location){for(int x=-1;x<=1;x++)for(int y=-1;y<=1;y++)for(int z=-1;z<=1;z++){UUID owner=lavaOwners.get(blockKey(location.getWorld().getBlockAt(location.getBlockX()+x,location.getBlockY()+y,location.getBlockZ()+z)));if(owner!=null)return owner;}return null;}
}
