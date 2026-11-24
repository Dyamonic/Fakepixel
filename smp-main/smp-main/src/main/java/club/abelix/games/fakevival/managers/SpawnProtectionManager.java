package club.abelix.games.fakevival.managers;

import club.abelix.games.fakevival.data.DataManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class SpawnProtectionManager {
    private final JavaPlugin plugin;
    private final DataManager dataManager;
    private final Set<UUID> buildMode = new HashSet<>();
    private Location pos1;
    private Location pos2;

    public SpawnProtectionManager(JavaPlugin plugin, DataManager dataManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
        load();
    }

    public void load() {
        pos1 = loadLocation("spawnprotection.pos1");
        pos2 = loadLocation("spawnprotection.pos2");
    }

    public void setPos1(Location location) {
        pos1 = location;
        saveLocation("spawnprotection.pos1", location);
    }

    public void setPos2(Location location) {
        pos2 = location;
        saveLocation("spawnprotection.pos2", location);
    }

    public boolean isReady() {
        return pos1 != null && pos2 != null && pos1.getWorld() != null && pos2.getWorld() != null && pos1.getWorld().equals(pos2.getWorld());
    }

    public boolean isInside(Location location) {
        if (!club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getBoolean("spawnprotection.enabled", true) || location == null || location.getWorld() == null) return false;
        if (isProtectedLobbyWorld(location.getWorld())) return true;
        if (isSpawnWorld(location)) return true;
        if (!isReady()) return false;
        if (!location.getWorld().equals(pos1.getWorld())) return false;
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

    public boolean canBypass(Player player) {
        return player != null && (isBuildMode(player) || player.hasPermission("fakevival.admin.spawnprotection.bypass"));
    }

    public boolean canBuildInHub(Player player) {
        return player != null && isBuildMode(player)
                && (player.hasPermission("smp.admin.buildmode") || player.hasPermission("fakevival.admin.buildmode"));
    }

    public boolean isHub(Location location) {
        return location != null && isProtectedLobbyWorld(location.getWorld());
    }

    public boolean isProtectedLobbyWorld(World world) {
        if(world==null)return false;
        if(plugin instanceof club.abelix.games.fakevival.Game game){
            try{
                if(game.getNetworkService().isHub())return world.getName().equalsIgnoreCase("hub")||world.getName().equalsIgnoreCase(getSpawnWorldName());
                if(game.getNetworkService().isDungeon())return world.getName().equalsIgnoreCase("lobby");
                return false;
            }catch(IllegalStateException ignored){ }
        }
        return world.getName().equalsIgnoreCase(getSpawnWorldName());
    }

    public void clearBuildMode(Player player) {
        if (player != null) buildMode.remove(player.getUniqueId());
    }

    public boolean isSpawnWorld(Location location) {
        if (location == null || location.getWorld() == null) return false;
        return location.getWorld().getName().equalsIgnoreCase(getSpawnWorldName());
    }

    public String getSpawnWorldName() {
        return club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString("spawnprotection.spawn-world", club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString("rtp.spawn-world", "hub"));
    }

    public boolean isBuildMode(Player player) {
        return player != null && buildMode.contains(player.getUniqueId());
    }

    public boolean toggleBuildMode(Player player) {
        if (buildMode.remove(player.getUniqueId())) return false;
        buildMode.add(player.getUniqueId());
        return true;
    }

    public boolean setBuildMode(Player player, boolean enabled) {
        if (player == null) return false;
        if (enabled) {
            buildMode.add(player.getUniqueId());
            return true;
        }
        buildMode.remove(player.getUniqueId());
        return false;
    }

    public String getStatus() {
        String spawnWorld = Bukkit.getWorld(getSpawnWorldName()) == null ? "&cMissing" : "&aEnabled";
        String cuboid = isReady() ? "&aSet in world &f" + pos1.getWorld().getName() : "&cNot set";
        return "&7Spawn world: " + spawnWorld + " &7(&f" + getSpawnWorldName() + "&7), Cuboid: " + cuboid;
    }

    private void saveLocation(String path, Location location) {
        dataManager.getData().set(path + ".world", location.getWorld().getName());
        dataManager.getData().set(path + ".x", location.getX());
        dataManager.getData().set(path + ".y", location.getY());
        dataManager.getData().set(path + ".z", location.getZ());
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
                dataManager.getData().getDouble(path + ".z"));
    }
}
