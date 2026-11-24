package club.abelix.games.fakevival.dungeonworld;

import club.abelix.games.fakevival.smpworlds.SMPWorldInstance;
import club.abelix.games.fakevival.smpworlds.SMPWorldManager;
import org.bukkit.World;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public final class SMPWorldDungeonProvider implements DungeonWorldProvider {
    private final SMPWorldManager worldManager;
    private final Map<String, String> dungeonToInstance = new ConcurrentHashMap<>();
    private final Map<String, String> instanceToWorld = new ConcurrentHashMap<>();

    public SMPWorldDungeonProvider(SMPWorldManager worldManager) {
        this.worldManager = worldManager;
    }

    @Override
    public CompletableFuture<DungeonWorldInstance> createInstance(String dungeonId) {
        return createInstance(dungeonId, dungeonId);
    }

    public CompletableFuture<DungeonWorldInstance> createInstance(String dungeonId, String templateWorldId) {
        String templateId = templateWorldId.toLowerCase(java.util.Locale.ROOT);
        return worldManager.createDungeonInstance(templateId).thenApply(instance -> {
            dungeonToInstance.put(dungeonId.toLowerCase(java.util.Locale.ROOT), instance.instanceId());
            instanceToWorld.put(instance.instanceId(), instance.worldId());
            return convert(instance);
        });
    }

    @Override
    public CompletableFuture<DungeonWorldInstance> loadInstance(String instanceId) {
        String worldId = instanceToWorld.getOrDefault(instanceId, instanceId);
        return worldManager.loadWorld(worldId).thenApply(world -> {
            instanceToWorld.put(instanceId, world.worldId());
            return new DungeonWorldInstance(instanceId, world.worldId(), world.bukkitWorld());
        });
    }

    @Override
    public CompletableFuture<Void> unloadInstance(String instanceId, boolean save) {
        String worldId = instanceToWorld.getOrDefault(instanceId, instanceId);
        return worldManager.unloadWorld(worldId, save).thenApply(ignored -> null);
    }

    @Override
    public CompletableFuture<Void> deleteInstance(String instanceId) {
        String worldId = instanceToWorld.remove(instanceId);
        if (worldId == null) worldId = instanceId;
        String deleteWorldId = worldId;
        dungeonToInstance.values().removeIf(value -> value.equals(instanceId));
        return worldManager.deleteWorld(deleteWorldId).thenApply(ignored -> null);
    }

    @Override
    public World getWorld(String instanceId) {
        String worldId = instanceToWorld.getOrDefault(instanceId, instanceId);
        return worldManager.getBukkitWorld(worldId);
    }

    @Override
    public boolean isLoaded(String instanceId) {
        String worldId = instanceToWorld.getOrDefault(instanceId, instanceId);
        return worldManager.isLoaded(worldId);
    }

    @Override
    public String getBackendName() {
        return "SMPWorlds";
    }

    public String activeInstanceId(String dungeonId) {
        return dungeonToInstance.get(dungeonId.toLowerCase(java.util.Locale.ROOT));
    }

    private DungeonWorldInstance convert(SMPWorldInstance instance) {
        return new DungeonWorldInstance(instance.instanceId(), instance.worldId(), instance.world());
    }
}
