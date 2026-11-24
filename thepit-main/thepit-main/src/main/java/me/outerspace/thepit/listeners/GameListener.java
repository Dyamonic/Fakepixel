package me.outerspace.thepit.listeners;

import me.outerspace.thepit.ThePitPlugin;
import me.outerspace.thepit.util.SecureItemTag;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.GameMode;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Item;
import org.bukkit.event.*;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.event.weather.ThunderChangeEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import org.bukkit.util.Vector;

public class GameListener implements Listener {
    private final ThePitPlugin plugin;
    private final Set<UUID> dropped = new HashSet<>();
    private final Set<UUID> fallImmune = new HashSet<>();
    private final Set<UUID> voidKilled = new HashSet<>();
    private final Set<UUID> forcedSelfDeaths = new HashSet<>();
    private final Set<UUID> kitRespawns = new HashSet<>();
    private final Map<Block, TemporaryBlock> temporaryBlocks = new HashMap<>();
    private final BukkitTask blockExpiryTask;
    private final Set<BukkitTask> arenaTasks = new HashSet<>();
    private final Map<UUID, Long> grapplingCooldown = new HashMap<>();
    private final Map<UUID, Long> launcherCooldowns = new HashMap<>();
    private final Map<UUID, UUID> hotPotatoHolder = new HashMap<>();

    public GameListener(ThePitPlugin plugin) {
        this.plugin = plugin;
        blockExpiryTask = Bukkit.getScheduler().runTaskTimer(plugin, this::expirePlacedBlocks, 10L, 10L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent e) {
        e.setJoinMessage(null);
        Player p = e.getPlayer();
        plugin.getPlayerInventoryManager().beginJoin(p);
        dropped.remove(p.getUniqueId());
        fallImmune.remove(p.getUniqueId());
        voidKilled.remove(p.getUniqueId());
        forcedSelfDeaths.remove(p.getUniqueId());
        plugin.getStatsManager().initializePlayer(p);
        p.setGameMode(GameMode.ADVENTURE);
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        Player dead = e.getEntity();
        boolean forcedSelfDeath = forcedSelfDeaths.remove(dead.getUniqueId());
        boolean playerKill = !forcedSelfDeath && dead.getKiller() != null && dead.getKiller() != dead;
        boolean voidDeath = dead.getLocation().getY() <= plugin.registry().settings().getInt("settings.void-y", 55);
        if (playerKill || voidDeath) kitRespawns.add(dead.getUniqueId());
        else kitRespawns.remove(dead.getUniqueId());
        if (plugin.getAchievementManager() != null) plugin.getAchievementManager().resetLife(dead.getUniqueId());
        if (plugin.getShopManager() != null) plugin.getShopManager().beforeDeath(dead);
        e.getDrops().clear();
        if (plugin.getFishingManager() != null) plugin.getFishingManager().captureAquaDeath(dead);
        dead.getInventory().clear();
        dead.getInventory().setArmorContents(null);
        plugin.getPlayerInventoryManager().captureDeathState(dead);
        dropped.remove(dead.getUniqueId());
        if (playerKill) {
            boolean validKill = plugin.getStatsManager().handleKill(dead.getKiller(), dead);
            if (validKill && plugin.getActionBarManager() != null) plugin.getActionBarManager().confirmKill(dead.getKiller(), dead);
            e.setDeathMessage(deathMsg("death-killed", dead, dead.getKiller()));
        } else if (voidDeath) {
            plugin.getStatsManager().handleDeath(dead);
            if (plugin.getAchievementManager() != null) plugin.getAchievementManager().publish(
                    new me.outerspace.thepit.managers.AchievementManager.ProgressEvent(dead.getUniqueId(),
                            me.outerspace.thepit.managers.AchievementManager.EventType.VOID_DEATH,
                            1L, java.util.Collections.emptyMap(), false));
            e.setDeathMessage(deathMsg("death-void", dead, null));
        } else if (dead.getLastDamageCause() != null && dead.getLastDamageCause().getCause() == DamageCause.FALL) {
            plugin.getStatsManager().handleDeath(dead);
            e.setDeathMessage(deathMsg("death-fall-damage", dead, null));
        } else {
            plugin.getStatsManager().handleDeath(dead);
            e.setDeathMessage(deathMsg("death-generic", dead, null));
        }
        if (plugin.getContractManager() != null) plugin.getContractManager().resetLife(dead);
        Bukkit.getScheduler().runTaskLater(plugin, () -> { if (dead.isOnline()) dead.spigot().respawn(); }, 2L);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onRespawn(PlayerRespawnEvent e) {
        Location spawn = plugin.getArenaManager().getCurrentSpawn();
        Location tactical = plugin.getShopManager() == null ? null : plugin.getShopManager().consumeRespawnLocation(e.getPlayer());
        if (tactical != null) spawn = tactical;
        if (spawn != null) e.setRespawnLocation(spawn);
        final Player p = e.getPlayer();
        if (spawn == null) Bukkit.getScheduler().runTask(plugin, () -> plugin.getArenaManager().teleportToCurrentSpawn(p));
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            dropped.remove(p.getUniqueId());
            fallImmune.remove(p.getUniqueId());
            voidKilled.remove(p.getUniqueId());
            if (kitRespawns.remove(p.getUniqueId())) {
                p.getInventory().clear();
                p.getInventory().setArmorContents(null);
                plugin.getDefaultLoadoutManager().give(p);
            }
            if (plugin.getFishingManager() != null) plugin.getFishingManager().restoreAquaDeath(p);
            if (plugin.getShopManager() != null) plugin.getShopManager().afterRespawn(p);
            if (plugin.getKillstreakManager() != null) plugin.getKillstreakManager().afterRespawn(p);
            p.setGameMode(GameMode.ADVENTURE);
            if (plugin.getLeaderboardManager() != null) plugin.getLeaderboardManager().queueLeaderboardRefresh(p);
            if (plugin.getTablistManager() != null) plugin.getTablistManager().refreshBounty(p);
            plugin.getPlayerInventoryManager().captureAndQueue(p, "RESPAWN_COMPLETE");
        }, 2L);
    }

    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        Player p = e.getPlayer();
        if (!plugin.getPlayerInventoryManager().isReady(p.getUniqueId())) {
            plugin.getPlayerInventoryManager().logBlockedMovement(p);
            e.setTo(e.getFrom());
            return;
        }

        if (plugin.getArenaManager().isInCurrentSpawnProtection(p.getLocation())) {
            dropped.remove(p.getUniqueId());
            if (p.getGameMode() != GameMode.ADVENTURE && !me.outerspace.thepit.util.PermissionCompat.has(plugin, p, "thepit.admin.gamemode")) p.setGameMode(GameMode.ADVENTURE);
            return;
        }

        int voidY = plugin.registry().settings().getInt("settings.void-y", 55);
        if (p.getLocation().getY() <= voidY && !plugin.getArenaManager().isInCurrentSpawnProtection(p.getLocation())) {
            if (!voidKilled.contains(p.getUniqueId()) && !p.isDead() && p.getHealth() > 0.0D) {
                voidKilled.add(p.getUniqueId());
                p.setHealth(0.0D);
            }
            return;
        }

        if (dropped.contains(p.getUniqueId())) return;

        if (plugin.getArenaManager().isInCurrentPvpArena(p.getLocation())) {
            dropped.add(p.getUniqueId());
            fallImmune.add(p.getUniqueId());
            Bukkit.getScheduler().runTaskLater(plugin, () -> fallImmune.remove(p.getUniqueId()), plugin.registry().settings().getInt("settings.spawn-fall-protection-seconds", 8) * 20L);
            if (p.getGameMode() == GameMode.ADVENTURE) p.setGameMode(GameMode.SURVIVAL);
        }
    }


    @EventHandler(priority = EventPriority.HIGHEST)
    public void onProtectedSpawnItemDrop(PlayerDropItemEvent event) {
        if (!plugin.getArenaManager().isInCurrentSpawnProtection(event.getPlayer().getLocation())) return;
        if (!plugin.getDefaultLoadoutManager().isProtectedSpawnItem(event.getItemDrop().getItemStack())) return;
        event.setCancelled(true);
        Bukkit.getScheduler().runTaskLater(plugin, event.getPlayer()::updateInventory, 1L);
    }

    public void forceSelfDeath(Player player) {
        if (player == null || player.isDead() || player.getHealth() <= 0.0D || !forcedSelfDeaths.add(player.getUniqueId())) return;
        player.setLastDamageCause(new EntityDamageEvent(player, DamageCause.CUSTOM, Math.max(1.0D, player.getHealth())));
        player.setHealth(0.0D);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        event.setQuitMessage(null);
        UUID uuid = event.getPlayer().getUniqueId();
        plugin.getPlayerInventoryManager().onQuit(event.getPlayer());
        kitRespawns.remove(uuid);
        voidKilled.remove(uuid);
        forcedSelfDeaths.remove(uuid);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInitializationTeleportDiagnostic(PlayerTeleportEvent event) {
        plugin.getPlayerInventoryManager().traceInitializationTeleport("ThePitFinalMonitor", event,
                event.isCancelled() ? "cancelled after ThePit listeners or by an external listener" : "accepted after ThePit listeners");
    }


    @EventHandler(ignoreCancelled = true)
    public void onFarmlandTrample(PlayerInteractEvent e) {
        if (e.getAction() != Action.PHYSICAL) return;
        if (e.getClickedBlock() == null || e.getClickedBlock().getType() != Material.SOIL) return;
        if (plugin.getArenaManager().isInCurrentSpawnProtection(e.getClickedBlock().getLocation())
                || plugin.getArenaManager().isInCurrentPvpArena(e.getClickedBlock().getLocation())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onSpecialLauncherUse(PlayerInteractEvent e) {
        if (e.getItem() == null) return;
        Action action = e.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;
        Player p = e.getPlayer();
        if (!plugin.getArenaManager().isInCurrentPvpArena(p.getLocation())) return;

        String id = getShopItemId(e.getItem());
        if (id == null) return;

        if (id.equals("potato_launcher")) {
            e.setCancelled(true);
            if (!checkLauncherCooldown(p, id, plugin.registry().settings().getInt("settings.potato-launcher-cooldown-ms", 2500))) return;
            shootPotato(p);
            consumeOne(p, e.getItem());
        } else if (id.equals("melon_cannon")) {
            e.setCancelled(true);
            if (!checkLauncherCooldown(p, id, plugin.registry().settings().getInt("settings.melon-cannon-cooldown-ms", 5000))) return;
            shootMelonCannon(p);
            consumeOne(p, e.getItem());
        } else if (id.equals("pig_launcher")) {
            e.setCancelled(true);
            if (!checkLauncherCooldown(p, id, plugin.registry().settings().getInt("settings.pig-launcher-cooldown-ms", 4000))) return;
            shootPigLauncher(p);
            consumeOne(p, e.getItem());
        } else if (id.equals("hot_potato")) {
            e.setCancelled(true);
            if (!hotPotatoHolder.containsKey(p.getUniqueId())) {
                startHotPotato(p);
                consumeOne(p, e.getItem());
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onGrapplingHook(PlayerFishEvent e) {
        Player p = e.getPlayer();
        ItemStack hand = p.getItemInHand();
        if (!"grappling_hook".equals(getShopItemId(hand))) return;
        if (!plugin.getArenaManager().isInCurrentPvpArena(p.getLocation())) return;

        if (e.getState() == PlayerFishEvent.State.FISHING) return;
        long now = System.currentTimeMillis();
        long last = grapplingCooldown.getOrDefault(p.getUniqueId(), 0L);
        if (now - last < plugin.registry().settings().getInt("settings.grappling-hook-cooldown-ms", 3500)) return;
        grapplingCooldown.put(p.getUniqueId(), now);

        Location hook = e.getHook() == null ? null : e.getHook().getLocation();
        if (hook == null) return;
        Vector velocity = hook.toVector().subtract(p.getLocation().toVector());
        if (velocity.lengthSquared() < 0.01D) return;
        double multiplier = plugin.registry().settings().getDouble("settings.grappling-hook-power", 1.45D);
        velocity.normalize().multiply(multiplier);
        velocity.setY(Math.max(0.45D, velocity.getY() + plugin.registry().settings().getDouble("settings.grappling-hook-y-boost", 0.35D)));
        p.setVelocity(velocity);
    }

    private String getShopItemId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        if (item.getItemMeta().getLore() != null) {
            for (String line : item.getItemMeta().getLore()) {
                String stripped = org.bukkit.ChatColor.stripColor(line).toLowerCase(java.util.Locale.ENGLISH);
                if (stripped.startsWith("buildffa-shop:")) return stripped.substring("buildffa-shop:".length());
            }
        }
        if (item.getItemMeta().hasDisplayName()) {
            String name = org.bukkit.ChatColor.stripColor(item.getItemMeta().getDisplayName()).toLowerCase(java.util.Locale.ENGLISH);
            if (name.contains("potato launcher")) return "potato_launcher";
            if (name.contains("melon cannon")) return "melon_cannon";
            if (name.contains("pig launcher")) return "pig_launcher";
            if (name.contains("grappling hook")) return "grappling_hook";
            if (name.contains("hot potato")) return "hot_potato";
        }
        return null;
    }


    private boolean checkLauncherCooldown(Player p, String id, long cooldownMs) {
        String key = p.getUniqueId().toString() + ":" + id;
        UUID uuid = java.util.UUID.nameUUIDFromBytes(key.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        long now = System.currentTimeMillis();
        long last = launcherCooldowns.getOrDefault(uuid, 0L);
        if (now - last < cooldownMs) {
            long left = Math.max(1L, (cooldownMs - (now - last) + 999L) / 1000L);
            p.sendMessage("§cWait " + left + "s before using that again.");
            return false;
        }
        launcherCooldowns.put(uuid, now);
        return true;
    }

    private void shootPotato(Player shooter) {
        Item potato = shooter.getWorld().dropItem(shooter.getEyeLocation().add(shooter.getEyeLocation().getDirection()), new ItemStack(Material.POTATO_ITEM, 1));
        potato.setPickupDelay(200);
        potato.setVelocity(shooter.getEyeLocation().getDirection().normalize().multiply(plugin.registry().settings().getDouble("settings.potato-launcher-speed", 2.4D)));
        trackDamageItem(shooter, potato, plugin.registry().settings().getDouble("settings.potato-launcher-damage", 5.0D), false);
    }

    private void shootPigLauncher(Player shooter) {
        Item pork = shooter.getWorld().dropItem(shooter.getEyeLocation().add(shooter.getEyeLocation().getDirection()), new ItemStack(Material.PORK, 1));
        pork.setPickupDelay(200);
        pork.setVelocity(shooter.getEyeLocation().getDirection().normalize().multiply(plugin.registry().settings().getDouble("settings.pig-launcher-speed", 2.0D)));
        trackDamageItem(shooter, pork, plugin.registry().settings().getDouble("settings.pig-launcher-damage", 3.0D), true);
    }

    private void shootMelonCannon(Player shooter) {
        FallingBlock melon = shooter.getWorld().spawnFallingBlock(shooter.getEyeLocation().add(shooter.getEyeLocation().getDirection()), Material.MELON_BLOCK, (byte) 0);
        melon.setDropItem(false);
        melon.setVelocity(shooter.getEyeLocation().getDirection().normalize().multiply(plugin.registry().settings().getDouble("settings.melon-cannon-speed", 1.5D)));
        final int[] ticks = {0};
        BukkitTask task = new org.bukkit.scheduler.BukkitRunnable() {
            @Override
            public void run() {
                if (melon.isDead() || ticks[0]++ > 60 || melon.isOnGround()) {
                    Location loc = melon.getLocation();
                    try { melon.remove(); } catch (Throwable ignored) { }
                    showExplosionParticles(loc);
                    launchNearbyPlayers(loc, plugin.registry().settings().getDouble("settings.melon-cannon-launch-power", 1.25D));
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 1L, 1L);
        arenaTasks.add(task);
    }

    private void trackDamageItem(Player shooter, Item item, double damage, boolean explode) {
        final int[] ticks = {0};
        BukkitTask task = new org.bukkit.scheduler.BukkitRunnable() {
            @Override
            public void run() {
                if (item.isDead() || ticks[0]++ > 80) {
                    try { if (!item.isDead()) item.remove(); } catch (Throwable ignored) { }
                    cancel();
                    return;
                }
                for (Entity entity : item.getNearbyEntities(0.7D, 0.7D, 0.7D)) {
                    if (!(entity instanceof Player target)) continue;
                    if (target.equals(shooter) || plugin.getArenaManager().isInCurrentSpawnProtection(target.getLocation())) continue;
                    target.damage(damage, shooter);
                    if (explode) {
                        showExplosionParticles(item.getLocation());
                        launchNearbyPlayers(item.getLocation(), plugin.registry().settings().getDouble("settings.pig-launcher-launch-power", 0.75D));
                    }
                    try { item.remove(); } catch (Throwable ignored) { }
                    cancel();
                    return;
                }
                if (explode && item.isOnGround()) {
                    showExplosionParticles(item.getLocation());
                    launchNearbyPlayers(item.getLocation(), plugin.registry().settings().getDouble("settings.pig-launcher-launch-power", 0.75D));
                    try { item.remove(); } catch (Throwable ignored) { }
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 1L, 1L);
        arenaTasks.add(task);
    }

    private void startHotPotato(Player holder) {
        UUID id = holder.getUniqueId();
        hotPotatoHolder.put(id, id);
        holder.sendMessage("§cHot Potato! Hit another player before it explodes!");
        scheduleHotPotato(holder, id);
    }

    private void passHotPotato(Player from, Player to) {
        UUID potatoId = null;
        for (Map.Entry<UUID, UUID> entry : hotPotatoHolder.entrySet()) {
            if (entry.getValue().equals(from.getUniqueId())) {
                potatoId = entry.getKey();
                break;
            }
        }
        if (potatoId == null) return;
        hotPotatoHolder.put(potatoId, to.getUniqueId());
        from.sendMessage("§aYou passed the hot potato!");
        to.sendMessage("§cHot Potato! Pass it before it explodes!");
        scheduleHotPotato(to, potatoId);
    }

    private void scheduleHotPotato(Player holder, UUID potatoId) {
        int seconds = 5 + new java.util.Random().nextInt(6);
        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            UUID current = hotPotatoHolder.get(potatoId);
            if (current == null || !current.equals(holder.getUniqueId())) return;
            hotPotatoHolder.remove(potatoId);
            if (!holder.isOnline() || holder.isDead()) return;
            Location loc = holder.getLocation();
            for (int i = 0; i < 12; i++) {
                Item potato = loc.getWorld().dropItemNaturally(loc, new ItemStack(Material.POTATO_ITEM, 1));
                potato.setPickupDelay(200);
                BukkitTask cleanup = Bukkit.getScheduler().runTaskLater(plugin, () -> { try { if (!potato.isDead()) potato.remove(); } catch (Throwable ignored) { } }, 60L);
                arenaTasks.add(cleanup);
            }
            showExplosionParticles(loc);
            launchNearbyPlayers(loc, plugin.registry().settings().getDouble("settings.hot-potato-launch-power", 1.4D));
            holder.damage(plugin.registry().settings().getDouble("settings.hot-potato-damage", 10.0D));
        }, seconds * 20L);
        arenaTasks.add(task);
    }

    private boolean hasHotPotato(Player p) {
        return hotPotatoHolder.containsValue(p.getUniqueId());
    }

    private void showExplosionParticles(Location loc) {
        try {
            loc.getWorld().playEffect(loc, org.bukkit.Effect.EXPLOSION_LARGE, 0);
            loc.getWorld().playSound(loc, org.bukkit.Sound.EXPLODE, 1.0F, 1.0F);
        } catch (Throwable ignored) { }
    }

    private void consumeOne(Player p, ItemStack item) {
        if (item.getAmount() <= 1) p.setItemInHand(null);
        else item.setAmount(item.getAmount() - 1);
        p.updateInventory();
    }

    @EventHandler(ignoreCancelled = true)
    public void onFireballUse(PlayerInteractEvent e) {
        if (plugin.registry().settings().getBoolean("settings.remove-fireballs", true)) return;
        if (e.getItem() == null || e.getItem().getType() != Material.FIREBALL) return;
        Action action = e.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;
        Player p = e.getPlayer();
        if (!plugin.getArenaManager().isInCurrentPvpArena(p.getLocation())) return;

        e.setCancelled(true);
        org.bukkit.inventory.ItemStack item = e.getItem();
        if (item.getAmount() <= 1) p.setItemInHand(null);
        else item.setAmount(item.getAmount() - 1);

        Fireball fireball = p.launchProjectile(Fireball.class);
        fireball.setIsIncendiary(false);
        fireball.setYield((float) plugin.registry().settings().getDouble("settings.bedwars-fireball-yield", 2.0D));
        fireball.setVelocity(p.getEyeLocation().getDirection().normalize().multiply(
                plugin.registry().settings().getDouble("settings.bedwars-fireball-speed", 1.6D)));
        p.updateInventory();
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent e) {
        if (plugin.isBuildMode(e.getPlayer())) return;
        Player player = e.getPlayer();
        Block block = e.getBlockPlaced();
        Material placedMaterial = block.getType();
        String source = temporaryBlockSource(e.getItemInHand(), placedMaterial);
        boolean validArena = plugin.getArenaManager().getCurrentWorld() != null
                && player.getWorld() == plugin.getArenaManager().getCurrentWorld()
                && block.getWorld() == plugin.getArenaManager().getCurrentWorld()
                && plugin.getArenaManager().isInCurrentPvpArena(player.getLocation())
                && plugin.getArenaManager().isInCurrentPvpArena(block.getLocation())
                && !plugin.getArenaManager().isInCurrentSpawnProtection(block.getLocation())
                && !isProtectedFeatureLocation(block.getLocation());
        boolean switching = plugin.getMapRotationManager() != null && plugin.getMapRotationManager().isSwitching();
        if (source == null || !validArena || switching || !eventAllowsTemporaryBlocks()) {
            e.setCancelled(true);
            player.sendMessage("§cYou cannot place that block here.");
            player.updateInventory();
            return;
        }
        BlockState replaced = e.getBlockReplacedState();
        double multiplier = 1.0D;
        if (plugin.getPassiveManager() != null) multiplier += plugin.getPassiveManager().bonus(player.getUniqueId(), "build-battler");
        if (plugin.getKillstreakManager() != null) multiplier *= plugin.getKillstreakManager().blockDurationMultiplier(player.getUniqueId());
        long configured = plugin.getShopManager() == null ? -1L : plugin.getShopManager().blockLifetimeSeconds(e.getItemInHand());
        if (configured <= 0L && plugin.getKillstreakManager() != null) configured = plugin.getKillstreakManager().blockLifetimeSeconds(e.getItemInHand());
        if (configured <= 0L) configured = plugin.registry().settings().getLong("temporary-blocks.sources." + source + ".lifetime-seconds",
                plugin.registry().settings().getLong("settings.placed-blocks-remove-seconds", 30L));
        long duration = Math.max(1L, configured * 1000L);
        long delay = Math.max(50L, Math.round(duration * multiplier));
        temporaryBlocks.put(block, new TemporaryBlock(replaced.getType(), replaced.getRawData(), placedMaterial,
                block.getData(), System.currentTimeMillis() + delay, plugin.getArenaManager().getCurrentArena(), player.getUniqueId(), source));
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockBreak(BlockBreakEvent e) {
        Location loc = e.getBlock().getLocation();

        if (!plugin.isBuildMode(e.getPlayer()) && temporaryBlocks.containsKey(e.getBlock())) {
            e.setCancelled(true);
            restoreTemporaryBlock(e.getBlock());
            e.getPlayer().updateInventory();
            return;
        }

        if (plugin.getArenaManager().isInCurrentSpawnProtection(loc)
                || plugin.getArenaManager().isInCurrentPvpArena(loc)) {
            if (!plugin.isBuildMode(e.getPlayer())) {
                e.setCancelled(true);
                e.getPlayer().sendMessage("§cYou cannot break arena blocks.");
                e.getPlayer().updateInventory();
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        if (e.getCause() == DamageCause.FALL) {
            e.setDamage(0.0D);
            e.setCancelled(true);
            return;
        }
        if (plugin.getArenaManager().isInCurrentSpawnProtection(p.getLocation())) e.setCancelled(true);
        if ((e.getCause() == DamageCause.ENTITY_EXPLOSION || e.getCause() == DamageCause.BLOCK_EXPLOSION)
                && plugin.registry().settings().getBoolean("settings.prevent-explosion-damage", true)) {
            e.setDamage(0.0D);
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerDamage(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof Player victim)) return;

        if ((e.getDamager() instanceof Fireball || e.getDamager() instanceof TNTPrimed)
                && plugin.registry().settings().getBoolean("settings.prevent-explosion-damage", true)) {
            e.setDamage(0.0D);
            e.setCancelled(true);
            launchNearbyPlayers(e.getDamager().getLocation(), plugin.registry().settings().getDouble("settings.explosion-launch-power", 1.15D));
            return;
        }

        if (!(e.getDamager() instanceof Player damager)) return;
        if (plugin.getArenaManager().isInCurrentSpawnProtection(victim.getLocation())
                || plugin.getArenaManager().isInCurrentSpawnProtection(damager.getLocation())) {
            e.setCancelled(true);
            return;
        }
        if (hasHotPotato(damager)) {
            e.setCancelled(true);
            passHotPotato(damager, victim);
        }
    }


    @EventHandler(ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent e) {
        if (plugin.getArenaManager().isInCurrentSpawnProtection(e.getLocation())
                || plugin.getArenaManager().isInCurrentPvpArena(e.getLocation())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onExplosionPrime(ExplosionPrimeEvent e) {
        if (plugin.getArenaManager().isInCurrentSpawnProtection(e.getEntity().getLocation())
                || plugin.getArenaManager().isInCurrentPvpArena(e.getEntity().getLocation())) {
            e.setFire(false);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent e) {
        if (plugin.getArenaManager().isInCurrentSpawnProtection(e.getLocation())
                || plugin.getArenaManager().isInCurrentPvpArena(e.getLocation())) {
            launchNearbyPlayers(e.getLocation(), plugin.registry().settings().getDouble("settings.explosion-launch-power", 1.15D));
            e.blockList().clear();
            e.setYield(0.0F);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockIgnite(BlockIgniteEvent e) {
        if (plugin.getArenaManager().isInCurrentSpawnProtection(e.getBlock().getLocation())
                || plugin.getArenaManager().isInCurrentPvpArena(e.getBlock().getLocation())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onTemporaryBlockPistonExtend(BlockPistonExtendEvent event) {
        for (Block block : event.getBlocks()) {
            if (temporaryBlocks.containsKey(block) || temporaryBlocks.containsKey(block.getRelative(event.getDirection()))) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onTemporaryBlockPistonRetract(BlockPistonRetractEvent event) {
        if (temporaryBlocks.containsKey(event.getRetractLocation().getBlock())) event.setCancelled(true);
    }





    @EventHandler(ignoreCancelled = true)
    public void onWeatherChange(WeatherChangeEvent e) {
        if (e.toWeatherState()) e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onThunderChange(ThunderChangeEvent e) {
        if (e.toThunderState()) e.setCancelled(true);
    }

    private void launchNearbyPlayers(Location center, double power) {
        double radius = plugin.registry().settings().getDouble("settings.explosion-launch-radius", 5.0D);
        for (Entity entity : center.getWorld().getNearbyEntities(center, radius, radius, radius)) {
            if (!(entity instanceof Player player)) continue;
            if (plugin.getArenaManager().isInCurrentSpawnProtection(player.getLocation())) continue;

            Vector direction = player.getLocation().toVector().subtract(center.toVector());
            if (direction.lengthSquared() < 0.01D) direction = new Vector(0, 1, 0);
            direction.normalize();
            direction.setY(Math.max(0.55D, direction.getY() + 0.45D));
            player.setVelocity(direction.multiply(power));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBanCommand(PlayerCommandPreprocessEvent e) {
        if (!plugin.registry().settings().getBoolean("settings.wipe-stats-on-ban", true)) return;
        handlePossibleBanCommand(e.getMessage());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onConsoleBanCommand(ServerCommandEvent e) {
        if (!plugin.registry().settings().getBoolean("settings.wipe-stats-on-ban", true)) return;
        handlePossibleBanCommand(e.getCommand());
    }

    private void handlePossibleBanCommand(String raw) {
        if (raw == null) return;
        String command = raw.trim();
        if (command.startsWith("/")) command = command.substring(1);
        if (command.isEmpty()) return;
        String[] parts = command.split("\\s+");
        if (parts.length < 2) return;
        String label = parts[0].toLowerCase();
        if (label.contains(":")) label = label.substring(label.indexOf(':') + 1);
        if (!label.equals("ban") && !label.equals("tempban") && !label.equals("ipban") && !label.equals("banip") && !label.equals("ban-ip")) return;
        final String targetName = parts[1];
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            org.bukkit.OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
            if (plugin.getStatsManager().resetPlayer(target)) {
                plugin.getLogger().info("Wiped The Pit statistics for banned player " + targetName + ".");
            }
        }, 1L);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChat(AsyncPlayerChatEvent e) {
        String format = plugin.getPlayerNameFormatter().resolve(e.getPlayer(), plugin.getNetworkProfileCache().get(e.getPlayer())).chatDisplay()
                + "&7: &f%message%";
        String safeFormat = format.replace("%", "%%")
                .replace("%%message%%", e.getMessage().replace("%", "%%"));
        e.setFormat(me.outerspace.thepit.util.CC.color(safeFormat));
    }

    private String deathMsg(String path, Player dead, Player killer) {
        String message = plugin.registry().messages().getString("messages." + path, "");
        String victimPrefix = plugin.getNetworkPrefix(dead);
        String formattedKiller = killer == null ? "" : plugin.getPlayerNameFormatter().combatName(killer);
        if (killer != null) {
            message = message.replace("%killer_abelix_prefix%%killer%", formattedKiller)
                    .replace("%abelix_prefix%%killer%", formattedKiller)
                    .replace("%killer%", formattedKiller);
        }
        message = message.replace("%victim_abelix_prefix%", victimPrefix == null ? "" : victimPrefix)
                .replace("%abelix_prefix%", victimPrefix == null ? "" : victimPrefix)
                .replace("%killer_abelix_prefix%", "");
        message = plugin.replaceNetworkPlaceholders(dead, killer, message);
        message = message.replace("%victim_abelix_prefix%", "").replace("%killer_abelix_prefix%", "").replace("%abelix_prefix%", "");
        return me.outerspace.thepit.util.CC.color(message);
    }

    public void clearPlacedBlocks(org.bukkit.World world) {
        for (Block block : new HashSet<>(temporaryBlocks.keySet())) {
            if (world == null || block.getWorld() == world) {
                restoreTemporaryBlock(block);
            }
        }
        for (BukkitTask task : new HashSet<>(arenaTasks)) task.cancel();
        arenaTasks.clear();
        hotPotatoHolder.clear();
    }

    public boolean isPlayerPlaced(Block block) {
        return block != null && temporaryBlocks.containsKey(block);
    }

    public int breakPlayerObsidian(Block start, int connectedAboveLimit) {
        if (start == null || start.getType() != Material.OBSIDIAN || !temporaryBlocks.containsKey(start)) return 0;
        int broken = 0;
        Block current = start;
        for (int index = 0; index <= Math.max(0, connectedAboveLimit); index++) {
            if (current.getType() != Material.OBSIDIAN || !temporaryBlocks.containsKey(current)) break;
            restoreTemporaryBlock(current);
            broken++;
            current = current.getRelative(0, 1, 0);
        }
        return broken;
    }

    private void expirePlacedBlocks() {
        long now = System.currentTimeMillis();
        for (Map.Entry<Block, TemporaryBlock> entry : new HashMap<>(temporaryBlocks).entrySet()) {
            if (entry.getValue().expiresAt() > now) continue;
            restoreTemporaryBlock(entry.getKey());
        }
    }

    @EventHandler
    public void onTemporaryBlockWorldUnload(WorldUnloadEvent event) {
        plugin.getArenaManager().onWorldUnload(event.getWorld());
        clearPlacedBlocks(event.getWorld());
    }

    public void shutdown() {
        blockExpiryTask.cancel();
        clearPlacedBlocks(null);
    }

    private void restoreTemporaryBlock(Block block) {
        TemporaryBlock state = temporaryBlocks.remove(block);
        if (state == null) return;
        block.setType(state.originalMaterial());
        block.setData(state.originalData());
    }

    private String temporaryBlockSource(ItemStack item, Material material) {
        if (item == null || item.getType() == Material.AIR || !item.getType().isBlock() || item.getType() != material) return null;
        String id = SecureItemTag.string(item, "ThePitItem");
        if (id == null || id.isBlank()) return null;
        String normalized = id.trim().toUpperCase(Locale.ENGLISH).replace('-', '_');
        if ((normalized.equals("PIT_SHOP_OBSIDIAN") || normalized.equals("PIT_SHOP_OBSIDIAN_STACK")) && material == Material.OBSIDIAN) return "OBSIDIAN";
        if (normalized.equals("OBSIDIAN") && material == Material.OBSIDIAN) return normalized;
        if (normalized.equals("MINEMAN_COBBLESTONE") && material == Material.COBBLESTONE) return normalized;
        if (normalized.equals("EVENT_BLOCK") || normalized.equals("REGISTERED_TEMPORARY_BLOCK")) {
            Material approved = Material.matchMaterial(plugin.registry().settings().getString("temporary-blocks.sources." + normalized + ".material", ""));
            return approved == material ? normalized : null;
        }
        return null;
    }

    private boolean eventAllowsTemporaryBlocks() {
        if (plugin.getMinorEventManager() == null || plugin.getMinorEventManager().activeEventId() == null) return true;
        return !plugin.registry().settings().getStringList("temporary-blocks.blocked-events")
                .contains(plugin.getMinorEventManager().activeEventId());
    }

    public boolean isProtectedFeatureLocation(Location location) {
        int radius = Math.max(1, plugin.registry().settings().getInt("temporary-blocks.feature-protection-radius", 3));
        if (plugin.getSlimeLaunchPadManager() != null && plugin.getSlimeLaunchPadManager().isProtectedLocation(location, radius)) return true;
        if (plugin.getMysticWellManager() != null && plugin.getMysticWellManager().isProtectedLocation(location, radius)) return true;
        String arena = plugin.getArenaManager().getCurrentArena();
        for (me.outerspace.thepit.setup.PitSetupSnapshot.Npc npc : plugin.getSetupService().snapshot().npcs) {
            if (!npc.enabled || npc.location == null || npc.arenaId == null || !npc.arenaId.equalsIgnoreCase(arena)) continue;
            if (near(location, npc.location.x, npc.location.y, npc.location.z, radius)) return true;
        }
        for (me.outerspace.thepit.setup.PitSetupSnapshot.Leaderboard board : plugin.getSetupService().snapshot().leaderboards) {
            if (board.location == null || board.arenaId == null || !board.arenaId.equalsIgnoreCase(arena)) continue;
            if (near(location, board.location.x, board.location.y, board.location.z, radius)) return true;
        }
        return false;
    }

    private boolean near(Location location, double x, double y, double z, int radius) {
        return Math.abs(location.getX() - x) <= radius && Math.abs(location.getY() - y) <= radius
                && Math.abs(location.getZ() - z) <= radius;
    }

    private record TemporaryBlock(Material originalMaterial, byte originalData, Material placedMaterial,
                                  byte placedData, long expiresAt, String arenaId, UUID owner, String sourceId) {}
}
