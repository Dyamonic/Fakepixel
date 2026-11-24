package club.abelix.api.server.base.listener;

import club.abelix.api.FalconAPI;
import club.abelix.api.base.listener.YListener;
import club.abelix.api.nms.NmsApi;
import club.abelix.api.nms.entity.type.FakeEntity;
import lombok.NonNull;
import lombok.extern.log4j.Log4j2;
import lombok.val;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;

@Log4j2
public class WorldListener extends YListener {

    private final NmsApi nmsApi = FalconAPI.getNmsApi();

    public WorldListener(@NonNull Plugin plugin) {
        super(plugin);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onLoad(WorldLoadEvent event) {
        World world = event.getWorld();

        nmsApi.getWorldApi().loadWorldEntities(world);
    }

    /**
     * Purest black magic to remove all entities from the world, we dont know how does it work, but it works.
     */

    @EventHandler(priority = EventPriority.MONITOR)
    public void onUnload(WorldUnloadEvent event) {
        log.info("Starting to unload world: {}", event.getWorld().getName());
        World world = event.getWorld();

        val entities = nmsApi.getFakeEntityApi()
                .getTrackedEntities()
                .stream()
                .filter(e -> e.getLocation().getWorld().getName().equalsIgnoreCase(world.getName()))
                .toList();

        nmsApi.getWorldApi().unloadWorldEntities(world);

        if (entities.isEmpty()) {
            log.info("No entities to remove from world {} during unloading", world.getName());
            return;
        }

        log.warn(
                "World '{}' is unloading: removed {} entities. " +
                        "Consider cleaning up entities explicitly or attaching them to the game.",
                world.getName(),
                entities.size()
        );

        entities.forEach(e -> {
            try {
                e.remove();
            } catch (Exception exception) {
                log.error("Failed to remove entity {} from world {}: {}", e.getCustomName(), world.getName(), exception.getMessage());
            }
        });

        log.info("The world {} has been unloaded and all entities have been removed", world.getName());
    }

}
