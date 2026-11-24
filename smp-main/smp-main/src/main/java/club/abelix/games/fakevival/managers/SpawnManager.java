package club.abelix.games.fakevival.managers;

import club.abelix.games.fakevival.data.DataManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

public final class SpawnManager {
    private final DataManager dataManager;
    private Location spawn;

    public SpawnManager(JavaPlugin plugin, DataManager dataManager) {
        this.dataManager = dataManager;
    }

    public void loadSpawn() {
        if (!dataManager.getData().contains("spawn.world")) return;
        World world = Bukkit.getWorld(dataManager.getData().getString("spawn.world", ""));
        if (world == null) {
            spawn = null;
            return;
        }
        spawn = new Location(
                world,
                dataManager.getData().getDouble("spawn.x"),
                dataManager.getData().getDouble("spawn.y"),
                dataManager.getData().getDouble("spawn.z"),
                (float) dataManager.getData().getDouble("spawn.yaw"),
                (float) dataManager.getData().getDouble("spawn.pitch")
        );
    }

    public void setSpawn(Location location) {
        if (location == null || location.getWorld() == null) return;
        setCachedSpawn(location);
        dataManager.getData().set("spawn.world", location.getWorld().getName());
        dataManager.getData().set("spawn.x", location.getX());
        dataManager.getData().set("spawn.y", location.getY());
        dataManager.getData().set("spawn.z", location.getZ());
        dataManager.getData().set("spawn.yaw", location.getYaw());
        dataManager.getData().set("spawn.pitch", location.getPitch());
        dataManager.save();
    }

    public void setCachedSpawn(Location location) {
        if (location == null || location.getWorld() == null) return;
        spawn = location.clone();
    }

    public Location getSpawn() {
        return spawn;
    }
}
