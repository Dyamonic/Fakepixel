package club.abelix.games.fakevival.listeners;

import club.abelix.games.fakevival.managers.SpawnProtectionManager;
import club.abelix.games.fakevival.managers.ZoneManager;
import club.abelix.games.fakevival.util.CC;
import org.bukkit.GameMode;
import org.bukkit.Difficulty;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityInteractEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.EntityPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.ItemFrame;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.event.weather.ThunderChangeEvent;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.plugin.java.JavaPlugin;

public final class SpawnProtectionListener implements Listener {
    private final JavaPlugin plugin;
    private final SpawnProtectionManager manager;
    private final ZoneManager zoneManager;

    public SpawnProtectionListener(JavaPlugin plugin, SpawnProtectionManager manager, ZoneManager zoneManager) {
        this.plugin = plugin;
        this.manager = manager;
        this.zoneManager = zoneManager;
        plugin.getServer().getScheduler().runTask(plugin, this::applySurvivalWorldRules);
        applySpawnWorldRules();
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        if (isHubServer() && (event.getBlock().getType() == Material.SWEET_BERRY_BUSH
                || event.getBlock().getType() == Material.TURTLE_EGG)
                && !manager.canBuildInHub(event.getPlayer())) {
            event.setCancelled(true);
            return;
        }
        if (denyHubBuild(event.getPlayer(), event.getBlock().getLocation())) {
            event.setCancelled(true);
            return;
        }
        if (!club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getBoolean("spawnprotection.disable-block-breaking-placing", true)) return;
        if (!manager.isInside(event.getBlock().getLocation()) || manager.canBypass(event.getPlayer())) return;
        if (zoneManager.shouldOverrideSpawnBreak(event.getBlock())) {
            zoneManager.scheduleRegeneration(event.getBlock());
            return;
        }
        event.setCancelled(true);
        event.getPlayer().sendMessage(CC.color(club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString("messages.spawnprotection-deny", "&cYou cannot edit blocks in spawn protection.")));
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        if (denyHubBuild(event.getPlayer(), event.getBlock().getLocation())) {
            event.setCancelled(true);
            return;
        }
        if (!club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getBoolean("spawnprotection.disable-block-breaking-placing", true)) return;
        if (!manager.isInside(event.getBlock().getLocation()) || manager.canBypass(event.getPlayer())) return;
        if (zoneManager.shouldOverrideSpawnPlace(event.getBlock())) return;
        event.setCancelled(true);
        event.getPlayer().sendMessage(CC.color(club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString("messages.spawnprotection-deny", "&cYou cannot edit blocks in spawn protection.")));
    }

    @EventHandler
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        if (denyHubBuild(event.getPlayer(), event.getBlock().getLocation())) event.setCancelled(true);
    }

    @EventHandler
    public void onBucketFill(PlayerBucketFillEvent event) {
        if (denyHubBuild(event.getPlayer(), event.getBlock().getLocation())) event.setCancelled(true);
    }

    @EventHandler
    public void onHangingPlace(HangingPlaceEvent event) {
        if (event.getPlayer() != null && denyHubBuild(event.getPlayer(), event.getEntity().getLocation())) event.setCancelled(true);
    }

    @EventHandler
    public void onHangingBreak(HangingBreakByEntityEvent event) {
        if (event.getRemover() instanceof org.bukkit.entity.Player player
                && denyHubBuild(player, event.getEntity().getLocation())) event.setCancelled(true);
    }

    @EventHandler
    public void onEntityPlace(EntityPlaceEvent event) {
        if (event.getPlayer() != null && denyHubBuild(event.getPlayer(), event.getEntity().getLocation())) event.setCancelled(true);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        manager.clearBuildMode(event.getPlayer());
    }

    @EventHandler
    public void onMobSpawn(CreatureSpawnEvent event) {
        if (club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getBoolean("spawnprotection.allow-mob-spawning", false)) return;
        if (event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.CUSTOM) return;
        if (zoneManager.allowsMobSpawn(event.getLocation(), event.getEntityType())) return;
        if (manager.isInside(event.getLocation())) event.setCancelled(true);
    }

    @EventHandler
    public void onFoodChange(FoodLevelChangeEvent event) {
        if (!club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getBoolean("spawnprotection.disable-hunger", true)) return;
        if (!(event.getEntity() instanceof org.bukkit.entity.Player player)) return;
        if (event.getFoodLevel() >= player.getFoodLevel()) return;
        if (isVanillaHungerWorld(player.getWorld())) return;
        event.setCancelled(true);
        player.setFoodLevel(20);
        player.setSaturation(Math.max(player.getSaturation(), 20.0F));
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof org.bukkit.entity.Player player)) return;
        if (!manager.isInside(player.getLocation())) return;
        if (event.getCause() == EntityDamageEvent.DamageCause.FALL
                && club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getBoolean("spawnprotection.disable-fall-damage", true)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPvp(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof org.bukkit.entity.Player player
                && event.getEntity() instanceof org.bukkit.entity.ArmorStand
                && denyHubBuild(player, event.getEntity().getLocation())) {
            event.setCancelled(true);
            return;
        }
        if (!club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getBoolean("spawnprotection.disable-pvp", true)) return;
        if (!(event.getEntity() instanceof org.bukkit.entity.Player victim)) return;
        org.bukkit.entity.Player attacker = getAttackingPlayer(event);
        if (attacker == null) return;
        if (zoneManager.isPvpBlocked(victim.getLocation()) || zoneManager.isPvpBlocked(attacker.getLocation())) {
            event.setCancelled(true);
            return;
        }
        if (zoneManager.allowsSpawnProtectionPvpOutsideZones()) return;
        if (!manager.isInside(victim.getLocation()) && !manager.isInside(attacker.getLocation())) return;
        event.setCancelled(true);
    }

    @EventHandler
    public void onFarmlandTrample(EntityChangeBlockEvent event) {
        if (isHubServer() && (event.getBlock().getType() == Material.TURTLE_EGG
                || event.getBlock().getType() == Material.SWEET_BERRY_BUSH)) {
            if (event.getEntity() instanceof org.bukkit.entity.Player player && manager.canBuildInHub(player)) return;
            event.setCancelled(true);
            return;
        }
        if (!club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getBoolean("spawnprotection.disable-farmland-trample", true)) return;
        if (event.getBlock().getType() != Material.FARMLAND) return;
        if (manager.isInside(event.getBlock().getLocation())) event.setCancelled(true);
    }

    @EventHandler
    public void onBedEnter(PlayerBedEnterEvent event) {
        if (!club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getBoolean("spawnprotection.disable-bed-sleep", true)) return;
        if (!manager.isInside(event.getBed().getLocation()) || manager.canBypass(event.getPlayer())) return;
        event.setCancelled(true);
        event.getPlayer().sendMessage(CC.color("&cYou cannot sleep in spawn."));
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (isHubServer() && event.getClickedBlock() != null) {
            Material clicked = event.getClickedBlock().getType();
            if ((clicked == Material.TURTLE_EGG || clicked == Material.SWEET_BERRY_BUSH)
                    && !manager.canBuildInHub(event.getPlayer())) {
                event.setCancelled(true);
                event.setUseInteractedBlock(org.bukkit.event.Event.Result.DENY);
                event.setUseItemInHand(org.bukkit.event.Event.Result.DENY);
                return;
            }
        }
        if (event.getClickedBlock() == null || !isProtectedInteraction(event.getClickedBlock().getLocation())) return;
        if (manager.isBuildMode(event.getPlayer()) && (event.getPlayer().hasPermission("smp.admin.buildmode") || event.getPlayer().hasPermission("fakevival.admin.buildmode"))) return;
        Material type = event.getClickedBlock().getType();
        if (club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getBoolean("spawnprotection.disable-container-access", true)
                && isProtectedContainer(type)) {
            event.setCancelled(true);
            event.setUseInteractedBlock(org.bukkit.event.Event.Result.DENY);
            event.setUseItemInHand(org.bukkit.event.Event.Result.DENY);
            event.getPlayer().sendMessage(CC.color("&cYou cannot interact with that here."));
            return;
        }
        if (club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getBoolean("spawnprotection.disable-bed-sleep", true)
                && event.getAction() == Action.RIGHT_CLICK_BLOCK
                && type.name().endsWith("_BED")) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(CC.color("&cYou cannot use beds in spawn."));
        }
    }

    @EventHandler
    public void onEntityPhysicalInteract(EntityInteractEvent event) {
        if (!isHubServer() || event.getBlock().getType() != Material.TURTLE_EGG) return;
        if (event.getEntity() instanceof org.bukkit.entity.Player player && manager.canBuildInHub(player)) return;
        event.setCancelled(true);
    }

    @EventHandler
    public void onProtectedEntityInteract(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof ItemFrame) && !(event.getRightClicked() instanceof ArmorStand)) return;
        if (!isProtectedInteraction(event.getRightClicked().getLocation())) return;
        if (manager.isBuildMode(event.getPlayer()) && (event.getPlayer().hasPermission("smp.admin.buildmode") || event.getPlayer().hasPermission("fakevival.admin.buildmode"))) return;
        event.setCancelled(true);
        event.getPlayer().sendMessage(CC.color("&cYou cannot interact with that here."));
    }

    @EventHandler
    public void onFireSpread(BlockSpreadEvent event) {
        if (club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getBoolean("spawnprotection.disable-fire-spread", true)
                && manager.isInside(event.getBlock().getLocation())
                && event.getSource().getType() == Material.FIRE) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onFireFade(BlockFadeEvent event) {
        if (!club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getBoolean("spawnprotection.disable-fire-spread", true)) return;
        if (manager.isInside(event.getBlock().getLocation()) && event.getBlock().getType() == Material.FIRE) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onLeavesDecay(LeavesDecayEvent event) {
        if (club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getBoolean("spawnprotection.disable-leaf-decay", true)
                && manager.isInside(event.getBlock().getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (event.getFrom().getBlockX() == event.getTo().getBlockX()
                && event.getFrom().getBlockY() == event.getTo().getBlockY()
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) return;
        applyAdventureMode(event.getPlayer());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        applySpawnWorldRules();
        applySurvivalWorldRules();
        plugin.getServer().getScheduler().runTask(plugin, () -> applyAdventureMode(event.getPlayer()));
    }

    @EventHandler
    public void onExplosionPrime(ExplosionPrimeEvent event) {
        if (!isProtectedExplosion(event.getEntity().getLocation())) return;
        event.setCancelled(true);
        debugExplosion(event.getEntity().getLocation(), true, true, 0);
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        boolean protectedOrigin = isProtectedExplosion(event.getLocation());
        if (protectedOrigin) {
            event.setCancelled(true);
            debugExplosion(event.getLocation(), true, true, event.blockList().size());
            return;
        }
        if (!event.getLocation().getWorld().getName().equalsIgnoreCase("world")) return;
        event.blockList().removeIf(block -> isProtectedExplosion(block.getLocation()));
        debugExplosion(event.getLocation(), false, event.isCancelled(), event.blockList().size());
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent event) {
        plugin.getServer().getScheduler().runTask(plugin, () -> applyAdventureMode(event.getPlayer()));
    }

    private void applyAdventureMode(org.bukkit.entity.Player player) {
        if (!club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getBoolean("spawnprotection.force-adventure", true)) return;
        if (!manager.isInside(player.getLocation()) || manager.canBypass(player)) return;
        if (zoneManager.bypassesAdventureMode(player.getLocation())) {
            if (player.getGameMode() == GameMode.ADVENTURE) player.setGameMode(GameMode.SURVIVAL);
            return;
        }
        if (player.getGameMode() != GameMode.ADVENTURE) player.setGameMode(GameMode.ADVENTURE);
    }

    private void applySpawnWorldRules() {
        World world = plugin.getServer().getWorld(manager.getSpawnWorldName());
        if (world != null) applyProtectedWorldRules(world);
        World lobby=plugin.getServer().getWorld("lobby");
        if(lobby!=null&&manager.isProtectedLobbyWorld(lobby))applyProtectedWorldRules(lobby);
    }

    private void applyProtectedWorldRules(World world) {
        if (club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getBoolean("spawnprotection.disable-fire-tick", true)) {
            world.setGameRule(org.bukkit.GameRules.FIRE_SPREAD_RADIUS_AROUND_PLAYER, 0);
        }
        if (club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).contains("spawnprotection.random-tick-speed")) {
            world.setGameRule(org.bukkit.GameRules.RANDOM_TICK_SPEED, club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getInt("spawnprotection.random-tick-speed", 0));
        }
        try {
            world.setDifficulty(Difficulty.valueOf(club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString("spawnprotection.difficulty", "EASY").toUpperCase(java.util.Locale.ROOT)));
        } catch (IllegalArgumentException ignored) {
            world.setDifficulty(Difficulty.EASY);
        }
        applyWeather(world);
        applyTime(world);
    }

    @EventHandler public void onWeather(WeatherChangeEvent event){if(event.toWeatherState()&&manager.isProtectedLobbyWorld(event.getWorld()))event.setCancelled(true);}
    @EventHandler public void onThunder(ThunderChangeEvent event){if(event.toThunderState()&&manager.isProtectedLobbyWorld(event.getWorld()))event.setCancelled(true);}

    private void applySurvivalWorldRules() {
        World world = plugin.getServer().getWorld("world");
        if (world != null) world.setGameRule(org.bukkit.GameRules.MOB_GRIEFING, true);
    }

    private boolean isProtectedExplosion(org.bukkit.Location location) {
        return manager.isInside(location) || zoneManager.blocksBuild(location);
    }

    private void debugExplosion(org.bukkit.Location location, boolean protectedArea, boolean cancelled, int blocks) {
        if (!club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getBoolean("debug", false)) return;
        plugin.getLogger().info("[SMP] Explosion check: world=" + location.getWorld().getName()
                + ", protected=" + protectedArea + ", cancelled=" + cancelled + ", blocks=" + blocks);
    }

    private void applyWeather(World world) {
        String weather = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString("spawnprotection.weather.type", "clear");
        if (!weather.equalsIgnoreCase("clear")) return;
        world.setStorm(false);
        world.setThundering(false);
        world.setWeatherDuration(20 * 60 * 60);
        world.setThunderDuration(0);
    }

    private void applyTime(World world) {
        String time = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString("spawnprotection.time", "day");
        if (time.equalsIgnoreCase("day")) {
            world.setTime(1000L);
        }
    }

    private boolean isProtectedContainer(Material type) {
        String name=type.name();
        return type == Material.CHEST
                || type == Material.TRAPPED_CHEST
                || type == Material.BARREL
                || type == Material.SHULKER_BOX
                || type.name().endsWith("_SHULKER_BOX")
                || type == Material.FURNACE
                || type == Material.BLAST_FURNACE
                || type == Material.SMOKER
                || type == Material.HOPPER
                || type == Material.DROPPER
                || type == Material.DISPENSER
                || type == Material.BREWING_STAND
                || type == Material.ENDER_CHEST
                || type == Material.LECTERN
                || type == Material.CAMPFIRE
                || type == Material.SOUL_CAMPFIRE
                || type == Material.ANVIL
                || type == Material.CHIPPED_ANVIL
                || type == Material.DAMAGED_ANVIL
                || type == Material.LEVER
                || name.endsWith("_DOOR")
                || name.endsWith("_TRAPDOOR")
                || name.endsWith("_FENCE_GATE")
                || name.endsWith("_BUTTON")
                || name.endsWith("_SIGN")
                || name.endsWith("_HANGING_SIGN");
    }

    private boolean isProtectedInteraction(org.bukkit.Location location){return manager.isInside(location)||zoneManager.blocksBuild(location);}

    private boolean denyHubBuild(org.bukkit.entity.Player player, org.bukkit.Location location) {
        if (!manager.isHub(location) || manager.canBuildInHub(player)) return false;
        return true;
    }

    public boolean isHubServer() {
        if (plugin instanceof club.abelix.games.fakevival.Game game && game.getNetworkService() != null) {
            String id = game.getNetworkService().getCurrentServerId();
            return game.getNetworkService().isHub() || id != null && id.toLowerCase(java.util.Locale.ROOT).startsWith("smp-");
        }
        String type = System.getenv().getOrDefault("SMP_SERVER_TYPE", "");
        String id = System.getenv().getOrDefault("SMP_SERVER_ID", "");
        return type.equalsIgnoreCase("HUB") || id.toLowerCase(java.util.Locale.ROOT).startsWith("smp-");
    }

    private boolean isVanillaHungerWorld(World world) {
        if (world == null) return false;
        java.util.List<String> worlds = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getStringList("spawnprotection.hunger-vanilla-worlds");
        if (worlds.isEmpty()) worlds = java.util.List.of("world");
        for (String name : worlds) {
            if (world.getName().equalsIgnoreCase(name)) return true;
        }
        return false;
    }

    private org.bukkit.entity.Player getAttackingPlayer(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof org.bukkit.entity.Player player) return player;
        if (event.getDamager() instanceof org.bukkit.entity.Projectile projectile) {
            ProjectileSource shooter = projectile.getShooter();
            if (shooter instanceof org.bukkit.entity.Player player) return player;
        }
        return null;
    }
}
