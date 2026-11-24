package club.abelix.api.server.base.listener;

import club.abelix.api.FalconAPI;
import club.abelix.api.base.listener.YListener;
import lombok.NonNull;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.plugin.Plugin;

/**
 * Временный фикс утечки в спиготе
 */
public class MetadataListener extends YListener {

    public MetadataListener(@NonNull Plugin plugin) {
        super(plugin);
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        Entity entity = event.getEntity();

        FalconAPI.getNmsApi().getWorldApi().removeEntityMetadata(entity);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();

        FalconAPI.getNmsApi().getWorldApi().removeBlockMetadata(block);
    }

}
