package net.fakepixel.housing.scoreboard;

import net.fakepixel.housing.HousingPlugin;
import net.fakepixel.housing.house.House;
import net.fakepixel.housing.house.HouseManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class CookieLeaderboard {
    private static final String LOBBY_HOLOGRAM_META = "housing_lobby_hologram";
    private final HousingPlugin plugin;
    private final HouseManager houses;
    private final List<ArmorStand> stands = new ArrayList<>();
    private int taskId = -1;

    public CookieLeaderboard(HousingPlugin plugin, HouseManager houses) {
        this.plugin = plugin;
        this.houses = houses;
    }

    public void start() {
        if (!plugin.getStaticConfig().getBoolean("cookie-leaderboard.enabled", true)) return;
        clearLobbyHolograms();
        refresh();
        taskId = new BukkitRunnable() {
            @Override public void run() { refresh(); }
        }.runTaskTimer(plugin, 40L, plugin.getStaticConfig().getLong("cookie-leaderboard.update-ticks", 1200L)).getTaskId();
    }

    public void stop() {
        if (taskId != -1) Bukkit.getScheduler().cancelTask(taskId);
        clear();
        clearLobbyHolograms();
    }

    public void refresh() {
        clear();
        Location base = getLocation();
        if (base == null) return;
        clearLobbyHolograms(base);
        spawnLine(base, 0, plugin.color(plugin.getStaticConfig().getString("cookie-leaderboard.title", "&bCookie Leaderboard")));
        Map<UUID, OwnerCookies> byOwner = new HashMap<>();
        for (House house : houses.getHouses()) {
            OwnerCookies entry = byOwner.get(house.getOwner());
            if (entry == null) {
                entry = new OwnerCookies(house.getOwner(), house.getOwnerName());
                byOwner.put(house.getOwner(), entry);
            }
            entry.cookies += house.getWeeklyCookies();
            if (house.getLastOpened() >= entry.lastOpened) {
                entry.lastOpened = house.getLastOpened();
                entry.ownerName = house.getOwnerName();
            }
        }
        List<OwnerCookies> top = new ArrayList<>(byOwner.values());
        top.sort(Comparator.comparingInt((OwnerCookies entry) -> entry.cookies).reversed());
        int limit = plugin.getStaticConfig().getInt("cookie-leaderboard.lines", 10);
        if (top.size() > limit) top = new ArrayList<>(top.subList(0, limit));
        int i = 1;
        for (OwnerCookies entry : top) {
            String name = latestName(entry);
            String prefix = plugin.getAbelixHook() == null ? "" : plugin.getAbelixHook().getPrefix(entry.owner, name);
            if (prefix == null || prefix.contains("%abelix_prefix%")) prefix = "";
            spawnLine(base, i, plugin.color("&e" + i + ". &r" + prefix + name + " &7- &e" + entry.cookies));
            i++;
        }
    }

    private String latestName(OwnerCookies entry) {
        org.bukkit.entity.Player online = Bukkit.getPlayer(entry.owner);
        if (online != null) return online.getName();
        try {
            String offline = Bukkit.getOfflinePlayer(entry.owner).getName();
            if (offline != null && !offline.trim().isEmpty()) return offline;
        } catch (Throwable ignored) {
        }
        return entry.ownerName == null || entry.ownerName.trim().isEmpty() ? "Unknown" : entry.ownerName;
    }

    private static final class OwnerCookies {
        private final UUID owner;
        private String ownerName;
        private int cookies;
        private long lastOpened = Long.MIN_VALUE;

        private OwnerCookies(UUID owner, String ownerName) {
            this.owner = owner;
            this.ownerName = ownerName;
        }
    }

    private Location getLocation() {
        String worldName = plugin.getStaticConfig().getString("cookie-leaderboard.world", plugin.getStaticConfig().getString("settings.lobby-world", "housinglobby"));
        World world = Bukkit.getWorld(worldName);
        if (world == null) return null;
        return new Location(world,
                plugin.getStaticConfig().getDouble("cookie-leaderboard.x", 0.5),
                plugin.getStaticConfig().getDouble("cookie-leaderboard.y", 80.0),
                plugin.getStaticConfig().getDouble("cookie-leaderboard.z", 0.5));
    }

    private void spawnLine(Location base, int index, String text) {
        Location loc = base.clone().subtract(0, index * 0.28, 0);
        ArmorStand stand = (ArmorStand) base.getWorld().spawnEntity(loc, EntityType.ARMOR_STAND);
        stand.setVisible(false);
        stand.setGravity(false);
        stand.setCanPickupItems(false);
        stand.setRemoveWhenFarAway(false);
        stand.setCustomNameVisible(true);
        tryCall(stand, "setInvisible", new Class[]{boolean.class}, true);
        tryCall(stand, "setInvulnerable", new Class[]{boolean.class}, true);
        tryCall(stand, "setCollidable", new Class[]{boolean.class}, false);
        tryCall(stand, "setSilent", new Class[]{boolean.class}, true);
        try { stand.setBasePlate(false); } catch (Throwable ignored) {}
        try { stand.setMarker(true); } catch (Throwable ignored) {}
        try { stand.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 1, false, false)); } catch (Throwable ignored) {}
        stand.setCustomName(text.length() > 64 ? text.substring(0, 64) : text);
        stand.setMetadata(LOBBY_HOLOGRAM_META, new FixedMetadataValue(plugin, true));
        stands.add(stand);
    }

    private void clear() {
        for (ArmorStand stand : stands) {
            if (stand != null && !stand.isDead()) stand.remove();
        }
        stands.clear();
    }

    private void clearLobbyHolograms() {
        Location base = getLocation();
        if (base != null) clearLobbyHolograms(base);
    }

    private void clearLobbyHolograms(Location base) {
        if (base == null || base.getWorld() == null) return;
        List<Entity> entities = new ArrayList<>(base.getWorld().getEntities());
        for (Entity entity : entities) {
            if (!(entity instanceof ArmorStand)) continue;
            ArmorStand stand = (ArmorStand) entity;
            if (isHousingLobbyHologram(stand, base)) {
                stands.remove(stand);
                stand.remove();
            }
        }
    }

    private boolean isHousingLobbyHologram(ArmorStand stand, Location base) {
        if (stand == null || stand.isDead() || base == null || base.getWorld() == null) return false;
        if (!stand.getWorld().equals(base.getWorld())) return false;
        if (stand.hasMetadata(LOBBY_HOLOGRAM_META)) return true;
        Location loc = stand.getLocation();
        if (Math.abs(loc.getX() - base.getX()) > 0.5 || Math.abs(loc.getZ() - base.getZ()) > 0.5) return false;
        int configuredLines = plugin.getStaticConfig().getInt("cookie-leaderboard.lines", 10) + 1;
        double minY = base.getY() - (Math.max(1, configuredLines) * 0.28) - 0.5;
        double maxY = base.getY() + 0.5;
        if (loc.getY() < minY || loc.getY() > maxY) return false;
        if (!stand.isCustomNameVisible() || stand.getCustomName() == null || stand.getCustomName().trim().isEmpty()) return false;
        return !stand.isVisible();
    }

    private void tryCall(Object target, String name, Class[] types, Object... args) {
        try {
            Method method = target.getClass().getMethod(name, types);
            method.invoke(target, args);
        } catch (Throwable ignored) {
        }
    }
}
