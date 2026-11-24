package club.abelix.games.fakevival.dungeonworld;

import club.abelix.games.fakevival.data.DataManager;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Locale;
import java.util.concurrent.CompletableFuture;

public final class SlimeWorldDungeonProvider implements DungeonWorldProvider {
    private final JavaPlugin plugin;
    private final DataManager dataManager;

    public SlimeWorldDungeonProvider(JavaPlugin plugin, DataManager dataManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
    }

    @Override
    public CompletableFuture<DungeonWorldInstance> createInstance(String dungeonId) {
        return CompletableFuture.completedFuture(instance(dungeonId));
    }

    @Override
    public CompletableFuture<DungeonWorldInstance> loadInstance(String instanceId) {
        return CompletableFuture.completedFuture(instance(instanceId));
    }

    @Override
    public CompletableFuture<Void> unloadInstance(String instanceId, boolean save) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> deleteInstance(String instanceId) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public World getWorld(String instanceId) {
        return Bukkit.getWorld(worldName(instanceId));
    }

    @Override
    public boolean isLoaded(String instanceId) {
        return getWorld(instanceId) != null;
    }

    @Override
    public String getBackendName() {
        return "SlimeWorldManager";
    }

    private DungeonWorldInstance instance(String dungeonId) {
        String id = normalize(dungeonId);
        return new DungeonWorldInstance(id, worldName(id), getWorld(id));
    }

    private String worldName(String dungeonId) {
        String path = "dungeons." + normalize(dungeonId) + ".world";
        return dataManager.getData().getString(path,
                club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString("dungeons.default-world", "dungeon"));
    }

    private String normalize(String value) {
        return value == null ? "dungeon" : value.toLowerCase(Locale.ROOT);
    }
}
