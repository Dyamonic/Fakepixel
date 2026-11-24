package club.abelix.games.fakevival.dungeonworld;

import org.bukkit.World;

import java.util.concurrent.CompletableFuture;

public interface DungeonWorldProvider {
    CompletableFuture<DungeonWorldInstance> createInstance(String dungeonId);
    CompletableFuture<DungeonWorldInstance> loadInstance(String instanceId);
    CompletableFuture<Void> unloadInstance(String instanceId, boolean save);
    CompletableFuture<Void> deleteInstance(String instanceId);
    World getWorld(String instanceId);
    boolean isLoaded(String instanceId);
    String getBackendName();
}
