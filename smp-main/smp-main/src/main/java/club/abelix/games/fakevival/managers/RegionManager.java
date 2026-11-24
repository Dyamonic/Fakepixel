package club.abelix.games.fakevival.managers;

import club.abelix.games.fakevival.data.DataManager;
import club.abelix.games.fakevival.util.CC;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class RegionManager {
    private final DataManager dataManager;
    private final TablistManager placeholders;
    private final Map<UUID, Set<String>> inside = new HashMap<>();

    public RegionManager(JavaPlugin plugin, DataManager dataManager, TablistManager placeholders) {
        this.dataManager = dataManager;
        this.placeholders = placeholders;
    }

    public void create(String name) {
        dataManager.getData().set(path(name) + ".name", name.toLowerCase());
        dataManager.getData().set(path(name) + ".display", pretty(name));
        dataManager.getData().set(path(name) + ".type", "village");
        dataManager.getData().set(path(name) + ".discover-message", "&aDiscovered &f%region%&a!");
        dataManager.save();
    }

    public boolean exists(String name) {
        return name != null && dataManager.getData().isConfigurationSection(path(name));
    }

    public void setPos(String name, String pos, Location location) {
        if (!exists(name)) create(name);
        saveLocation(path(name) + "." + pos.toLowerCase(), location);
    }

    public boolean isInside(String name, Location location) {
        Location pos1 = loadLocation(path(name) + ".pos1");
        Location pos2 = loadLocation(path(name) + ".pos2");
        if (pos1 == null || pos2 == null || location == null || location.getWorld() == null) return false;
        if (!location.getWorld().equals(pos1.getWorld()) || !pos1.getWorld().equals(pos2.getWorld())) return false;
        double minX = Math.min(pos1.getX(), pos2.getX());
        double maxX = Math.max(pos1.getX(), pos2.getX());
        double minY = Math.min(pos1.getY(), pos2.getY());
        double maxY = Math.max(pos1.getY(), pos2.getY());
        double minZ = Math.min(pos1.getZ(), pos2.getZ());
        double maxZ = Math.max(pos1.getZ(), pos2.getZ());
        return location.getX() >= minX && location.getX() <= maxX
                && location.getY() >= minY && location.getY() <= maxY
                && location.getZ() >= minZ && location.getZ() <= maxZ;
    }

    public void checkMove(Player player) {
        ConfigurationSection regions = dataManager.getData().getConfigurationSection("regions");
        if (regions == null) return;
        Set<String> current = inside.computeIfAbsent(player.getUniqueId(), ignored -> new HashSet<>());
        for (String region : regions.getKeys(false)) {
            boolean nowInside = isInside(region, player.getLocation());
            boolean wasInside = current.contains(region.toLowerCase());
            if (nowInside && !wasInside) {
                current.add(region.toLowerCase());
                discover(player, region);
            } else if (!nowInside && wasInside) {
                current.remove(region.toLowerCase());
            }
        }
    }

    private void discover(Player player, String region) {
        String base = path(region);
        String display = dataManager.getData().getString(base + ".display", pretty(region));
        String type = dataManager.getData().getString(base + ".type", "region");
        String discoveredPath = "players." + player.getUniqueId() + ".regions." + region.toLowerCase();
        boolean firstTime = !dataManager.getData().getBoolean(discoveredPath, false);
        if (firstTime) {
            dataManager.getData().set(discoveredPath, true);
            dataManager.save();
        }
        String message = dataManager.getData().getString(base + (firstTime ? ".discover-message" : ".enter-message"), firstTime ? "&aDiscovered &f%region%&a!" : "&7Entering &f%region%&7...");
        player.sendMessage(placeholders.parse(player, CC.color(message.replace("%region%", display).replace("%type%", type))));
        for (String command : dataManager.getData().getStringList(base + ".commands")) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), placeholders.parse(player, command.replace("%player%", player.getName()).replace("%region%", display).replace("%type%", type)));
        }
    }

    public String listRegions() {
        ConfigurationSection section = dataManager.getData().getConfigurationSection("regions");
        if (section == null || section.getKeys(false).isEmpty()) return "None";
        return String.join(", ", section.getKeys(false));
    }

    private String path(String name) {
        return "regions." + name.toLowerCase();
    }

    private void saveLocation(String path, Location location) {
        dataManager.getData().set(path + ".world", location.getWorld().getName());
        dataManager.getData().set(path + ".x", location.getX());
        dataManager.getData().set(path + ".y", location.getY());
        dataManager.getData().set(path + ".z", location.getZ());
        dataManager.getData().set(path + ".yaw", location.getYaw());
        dataManager.getData().set(path + ".pitch", location.getPitch());
        dataManager.save();
    }

    private Location loadLocation(String path) {
        String worldName = dataManager.getData().getString(path + ".world");
        if (worldName == null) return null;
        World world = Bukkit.getWorld(worldName);
        if (world == null) return null;
        return new Location(world,
                dataManager.getData().getDouble(path + ".x"),
                dataManager.getData().getDouble(path + ".y"),
                dataManager.getData().getDouble(path + ".z"),
                (float) dataManager.getData().getDouble(path + ".yaw"),
                (float) dataManager.getData().getDouble(path + ".pitch"));
    }

    private String pretty(String text) {
        if (text == null || text.isEmpty()) return "Region";
        return text.substring(0, 1).toUpperCase() + text.substring(1).toLowerCase();
    }
}
