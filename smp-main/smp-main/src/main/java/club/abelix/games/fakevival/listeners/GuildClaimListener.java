package club.abelix.games.fakevival.listeners;

import club.abelix.games.fakevival.managers.GuildManager;
import club.abelix.games.fakevival.util.CC;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.ArmorStand;

public final class GuildClaimListener implements Listener {
    private final GuildManager guildManager;

    public GuildClaimListener(GuildManager guildManager) {
        this.guildManager = guildManager;
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        if (guildManager.canBuild(event.getPlayer(), event.getBlock().getLocation())) return;
        event.setCancelled(true);
        event.getPlayer().sendMessage(CC.color("&cThis chunk is claimed by &f" + guildManager.getClaimOwnerName(event.getBlock().getLocation()) + "&c."));
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (guildManager.canBuild(event.getPlayer(), event.getBlock().getLocation())) return;
        event.setCancelled(true);
        event.getPlayer().sendMessage(CC.color("&cThis chunk is claimed by &f" + guildManager.getClaimOwnerName(event.getBlock().getLocation()) + "&c."));
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null) return;
        if (guildManager.canBuild(event.getPlayer(), event.getClickedBlock().getLocation())) return;
        event.setCancelled(true);
        event.getPlayer().sendMessage(CC.color("&cThis chunk is claimed by &f" + guildManager.getClaimOwnerName(event.getClickedBlock().getLocation()) + "&c."));
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityInteract(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof ItemFrame) && !(event.getRightClicked() instanceof ArmorStand)) return;
        if (guildManager.canBuild(event.getPlayer(), event.getRightClicked().getLocation())) return;
        event.setCancelled(true);
        event.getPlayer().sendMessage(CC.color("&cYou cannot interact with that here."));
    }
}
