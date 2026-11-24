package me.outerspace.thepit.managers;

import me.outerspace.thepit.ThePitPlugin;
import me.outerspace.thepit.util.CC;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public final class MapRotationManager {
    private final ThePitPlugin plugin;
    private final File file;
    private BukkitTask task;
    private String currentMap;
    private long rotationStart;
    private volatile boolean switching;

    public MapRotationManager(ThePitPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "settings.yml");
    }

    public boolean initialize() {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        List<String> maps = maps();
        if (maps.isEmpty()) {
            plugin.getLogger().severe("Map rotation has no configured maps.");
            return false;
        }
        currentMap = normalize(yaml.getString("map-rotation.current-map", maps.get(0)));
        if (!maps.contains(currentMap)) currentMap = maps.get(0);
        rotationStart = yaml.getLong("map-rotation.rotation-start", 0L);
        if (rotationStart <= 0L) {
            rotationStart = System.currentTimeMillis();
            return save(currentMap, rotationStart);
        }
        return true;
    }

    public void start() {
        stop();
        if (!enabled()) return;
        task = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::check, 1200L, 1200L);
    }

    public void stop() {
        if (task != null) task.cancel();
        task = null;
    }

    public void checkNow() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, this::check);
    }

    private void check() {
        if (!enabled() || switching) return;
        long duration = durationMillis();
        long now = System.currentTimeMillis();
        long elapsed = Math.max(0L, now - rotationStart);
        long periods = elapsed / duration;
        if (periods <= 0L) return;
        List<String> maps = maps();
        int current = maps.indexOf(currentMap);
        if (current < 0) current = 0;
        String target = maps.get((int) ((current + periods) % maps.size()));
        long nextStart = rotationStart + periods * duration;
        switchTo(target, nextStart, result -> {
        });
    }

    public void next(Consumer<Boolean> callback) {
        List<String> maps = maps();
        int index = maps.indexOf(currentMap);
        String target = maps.get((index < 0 ? 0 : index + 1) % maps.size());
        switchTo(target, System.currentTimeMillis(), callback);
    }

    public void set(String requested, Consumer<Boolean> callback) {
        String target = normalize(requested);
        if (!maps().contains(target)) {
            callback.accept(false);
            return;
        }
        switchTo(target, System.currentTimeMillis(), callback);
    }

    public boolean resetTime() {
        rotationStart = System.currentTimeMillis();
        return save(currentMap, rotationStart);
    }

    private void switchTo(String target, long newStart, Consumer<Boolean> callback) {
        if (switching) {
            callback.accept(false);
            return;
        }
        switching = true;
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (plugin.getCombatKnockbackCompatibility() != null) plugin.getCombatKnockbackCompatibility().clearAll();
            plugin.getArenaManager().changeArenaSafely(target, true, success -> {
            switching = false;
            if (success) {
                currentMap = target;
                rotationStart = newStart;
                if (!save(currentMap, rotationStart)) plugin.getLogger().severe("Map rotation switched to '" + target + "' but could not persist settings.yml.");
            }
            callback.accept(success);
            });
        });
    }

    public String currentMap() {
        return currentMap;
    }

    public long rotationStart() {
        return rotationStart;
    }

    public long remainingMillis() {
        long end = rotationStart + durationMillis();
        return Math.max(0L, end - System.currentTimeMillis());
    }

    public long remainingDays() {
        return remainingMillis() / 86400000L;
    }

    public long remainingHours() {
        return remainingMillis() / 3600000L;
    }

    public String remainingFormatted() {
        long minutes = Math.max(0L, remainingMillis() / 60000L);
        long days = minutes / 1440L;
        long hours = minutes % 1440L / 60L;
        long mins = minutes % 60L;
        if (days > 0L) return days + "d " + hours + "h";
        if (hours > 0L) return hours + "h " + mins + "m";
        return mins + "m";
    }

    public String status() {
        return CC.color("&7Map: &f" + plugin.getArenaManager().getDisplayName(currentMap)
                + " &8(&f" + currentMap + "&8) &7Remaining: &f" + remainingFormatted());
    }

    public List<String> maps() {
        List<String> configured = plugin.registry().settings().getStringList("map-rotation.maps");
        List<String> result = new ArrayList<>();
        for (String value : configured) {
            String id = normalize(value);
            if (!id.isEmpty() && !result.contains(id)) result.add(id);
        }
        return result;
    }

    public boolean enabled() {
        return plugin.registry().settings().getBoolean("map-rotation.enabled", true);
    }

    public boolean isSwitching() { return switching; }

    public long durationMillis() {
        long days = Math.max(1L, plugin.registry().settings().getLong("map-rotation.duration-days", 5L));
        return days * 86400000L;
    }

    private boolean save(String map, long start) {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        yaml.set("map-rotation.current-map", normalize(map));
        yaml.set("map-rotation.rotation-start", start);
        try {
            yaml.save(file);
            return true;
        } catch (IOException exception) {
            plugin.getLogger().severe("Could not save map rotation state: " + exception.getMessage());
            return false;
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ENGLISH);
    }
}
