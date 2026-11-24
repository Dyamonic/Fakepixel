package club.abelix.games.buildffa.game.listener.game;

import club.abelix.api.base.listener.YListener;
import club.abelix.games.buildffa.game.BuildFFAGame;
import lombok.val;
import org.bukkit.Material;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.jetbrains.annotations.NotNull;

/**
 * @author milansky
 */
public final class ExplosionListener extends YListener {
    private final BuildFFAGame game;

    public ExplosionListener(final @NotNull BuildFFAGame game) {
        super(game.getPlugin());
        this.game = game;
    }

    @EventHandler
    public void onExplosion(final @NotNull EntityExplodeEvent event) {
        event.blockList().clear();
    }

    @EventHandler
    public void onIgnite(final @NotNull BlockIgniteEvent event) {
        if (event.getCause() == BlockIgniteEvent.IgniteCause.FLINT_AND_STEEL) return;

        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(final @NotNull BlockPlaceEvent event) {
        val block = event.getBlockPlaced();

        if (block.getType() != Material.TNT) return;
        if (game.isBuildMode(event.getPlayer()) && game.getWorldConfig() != null
                && block.getWorld().equals(game.getWorldConfig().world())) return;

        event.setCancelled(true);

        val location = block.getLocation();
        location.getWorld().spawn(location, TNTPrimed.class);
    }
}
