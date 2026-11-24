package me.outerspace.thepit.managers;

import me.outerspace.thepit.ThePitPlugin;
import org.bukkit.Effect;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class SlimeLaunchPadManager implements Listener {
    private final ThePitPlugin plugin;
    private final Map<PadKey, ArenaFeatureConfiguration.LaunchPad> activePads = new HashMap<>();
    private final Map<String, ArenaFeatureConfiguration.LaunchPad> configuredPads = new LinkedHashMap<>();
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final Map<UUID, Long> fallProtection = new HashMap<>();
    private ArenaFeatureConfiguration.LaunchPads configuration;
    private String activeArena;

    public SlimeLaunchPadManager(ThePitPlugin plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        activePads.clear();
        configuredPads.clear();
        cooldowns.clear();
        fallProtection.clear();
        activeArena = plugin.getArenaManager().getCurrentArena();
        configuration = plugin.getYamlManager().launchPads(activeArena);
        if (activeArena == null || !configuration.enabled()) return;
        configuredPads.putAll(configuration.locations());
        for (ArenaFeatureConfiguration.LaunchPad pad : configuredPads.values()) {
            org.bukkit.World world = org.bukkit.Bukkit.getWorld(pad.worldName());
            if (world == null) {
                plugin.getLogger().warning("Launch pad '" + pad.id() + "' in arena '" + activeArena + "' references unloaded world '" + pad.worldName() + "'.");
                continue;
            }
            activePads.put(new PadKey(world.getUID(), pad.x(), pad.y(), pad.z()), pad);
        }
    }

    public void shutdown() {
        activePads.clear();
        configuredPads.clear();
        cooldowns.clear();
        fallProtection.clear();
        activeArena = null;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (configuration == null || !configuration.enabled() || event.getTo() == null) return;
        Player player = event.getPlayer();
        if (player.isDead() || player.getHealth() <= 0.0D || player.getGameMode() == GameMode.SPECTATOR || player.hasMetadata("NPC")) return;
        Block below = event.getTo().clone().subtract(0.0D, 0.1D, 0.0D).getBlock();
        if (below.getType() != Material.SLIME_BLOCK) return;
        ArenaFeatureConfiguration.LaunchPad pad = activePads.get(PadKey.of(below.getLocation()));
        if (pad == null) return;
        long now = System.currentTimeMillis();
        if (cooldowns.getOrDefault(player.getUniqueId(), 0L) > now) return;
        boolean apollo = plugin.getRenownShopManager() != null
                && plugin.getRenownShopManager().owns(player.getUniqueId(), "apollo")
                && plugin.getRenownShopManager().isToggleEnabled(player.getUniqueId(), "apollo");
        Vector direction;
        if (apollo) {
            direction = player.getEyeLocation().getDirection().setY(0.0D);
        } else {
            Location spawn = plugin.getArenaManager().getCurrentSpawn();
            direction = spawn == null || spawn.getWorld() != player.getWorld()
                    ? player.getEyeLocation().getDirection().setY(0.0D)
                    : below.getLocation().toVector().subtract(spawn.toVector()).setY(0.0D);
        }
        if (direction.lengthSquared() < 0.0001D) {
            double radians = Math.toRadians(player.getLocation().getYaw());
            direction = new Vector(-Math.sin(radians), 0.0D, Math.cos(radians));
        }
        player.setVelocity(direction.normalize().multiply(pad.horizontalPower()).setY(pad.verticalPower()));
        plugin.getStatsManager().addLongStat(player.getUniqueId(), StatsManager.LAUNCHER_LAUNCHES, 1L);
        cooldowns.put(player.getUniqueId(), now + Math.max(0L, pad.cooldownMillis()));
        fallProtection.put(player.getUniqueId(), now + Math.max(0L, pad.fallProtectionSeconds() * 1000L));
        playEffects(player.getLocation(), pad);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player) || event.getCause() != EntityDamageEvent.DamageCause.FALL) return;
        Player player = (Player) event.getEntity();
        Long until = fallProtection.get(player.getUniqueId());
        if (until == null) return;
        if (until >= System.currentTimeMillis()) event.setCancelled(true);
        else fallProtection.remove(player.getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        cooldowns.remove(event.getPlayer().getUniqueId());
        fallProtection.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        if (configuredPads.values().stream().anyMatch(pad -> pad.worldName().equals(event.getWorld().getName()))) reload();
    }

    @EventHandler
    public void onWorldUnload(WorldUnloadEvent event) {
        UUID worldId = event.getWorld().getUID();
        activePads.keySet().removeIf(key -> key.worldId().equals(worldId));
    }

    public String addPad(Player player, String requestedName) {
        String arena = activeArenaAt(player.getLocation());
        if (arena == null) return "§cYou must be in the active arena world.";
        Block block = player.getLocation().clone().subtract(0.0D, 0.1D, 0.0D).getBlock();
        if (block.getType() != Material.SLIME_BLOCK) return "§cThe block beneath you must be a slime block.";
        String id = requestedName == null || requestedName.trim().isEmpty() ? nextName() : PitYamlManager.normalizeStableId(requestedName);
        if (id.isEmpty()) return "§cNames may contain only a-z, 0-9, underscore, and hyphen.";
        if (configuredPads.containsKey(id)) return "§cA launch pad named §e" + id + " §calready exists.";
        if (activePads.containsKey(PadKey.of(block.getLocation()))) return "§eThat slime block is already registered.";
        if (!plugin.getYamlManager().saveLaunchPad(arena, id, block.getLocation(), configuration.defaults())) return unavailable();
        reload();
        return "§aLaunch pad §f" + id + " §awas added to arena §f" + arena + "§a.";
    }

    public String removePad(Player player, String requestedName) {
        String arena = activeArenaAt(player.getLocation());
        if (arena == null) return "§cYou must be in the active arena world.";
        String id = PitYamlManager.normalizeStableId(requestedName);
        if (id.isEmpty()) return "§cNames may contain only a-z, 0-9, underscore, and hyphen.";
        if (!configuredPads.containsKey(id)) return "§cNo launch pad named §e" + id + " §cexists in the active arena.";
        if (!plugin.getYamlManager().removeLaunchPad(arena, id)) return unavailable();
        reload();
        return "§aLaunch pad §f" + id + " §awas removed from arena §f" + arena + "§a.";
    }

    public List<String> listPads() {
        if (activeArena == null) return Collections.singletonList("§cNo active arena.");
        List<String> out = new ArrayList<>();
        out.add("§6Launch pads for §f" + activeArena + "§6: §e" + configuredPads.size());
        for (ArenaFeatureConfiguration.LaunchPad pad : configuredPads.values()) {
            out.add("§7" + pad.id() + ": §f" + pad.worldName() + "," + pad.x() + "," + pad.y() + "," + pad.z());
        }
        return out;
    }

    public List<String> locationIds() {
        return new ArrayList<>(configuredPads.keySet());
    }

    public boolean isProtectedLocation(Location location, int radius) {
        if (location == null || location.getWorld() == null) return false;
        int distance = Math.max(0, radius);
        for (PadKey key : activePads.keySet()) {
            if (!key.worldId().equals(location.getWorld().getUID())) continue;
            if (Math.abs(key.x() - location.getBlockX()) <= distance
                    && Math.abs(key.y() - location.getBlockY()) <= distance
                    && Math.abs(key.z() - location.getBlockZ()) <= distance) return true;
        }
        return false;
    }

    private String nextName() {
        int index = 1;
        while (configuredPads.containsKey("launchpad_" + index)) index++;
        return "launchpad_" + index;
    }

    private void playEffects(Location location, ArenaFeatureConfiguration.LaunchPad pad) {
        try {
            location.getWorld().playSound(location, Sound.valueOf(pad.sound().toUpperCase()), 1.0F, 1.0F);
        } catch (Throwable ignored) {
            try {
                location.getWorld().playSound(location, Sound.IRONGOLEM_HIT, 1.0F, 1.0F);
            } catch (Throwable ignoredAgain) {
            }
        }
        try {
            location.getWorld().playEffect(location, Effect.valueOf(pad.particle().toUpperCase()), 0);
        } catch (Throwable ignored) {
            try {
                location.getWorld().playEffect(location, Effect.STEP_SOUND, Material.SLIME_BLOCK.getId());
            } catch (Throwable ignoredAgain) {
            }
        }
    }

    private String activeArenaAt(Location location) {
        String arena = plugin.getArenaManager().getCurrentArena();
        if (arena == null || location.getWorld() == null) return null;
        String world = plugin.getArenaManager().getArena(arena).map(value -> value.worldName()).orElse("");
        return location.getWorld().getName().equals(world) ? arena : null;
    }

    private String unavailable() {
        return "§cCould not save arenas.yml. Nothing was changed.";
    }

    private record PadKey(UUID worldId, int x, int y, int z) {
        private static PadKey of(Location location) {
            return new PadKey(location.getWorld().getUID(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
        }
    }
}
