package club.abelix.games.buildffa.game.listener.game;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.listener.YListener;
import club.abelix.games.buildffa.game.BuildFFAGame;
import club.abelix.games.buildffa.game.manager.event.EventItems;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.PluginDisableEvent;

public class BlockPlaceListener extends YListener implements Listener {
    private final BuildFFAGame game;

    public BlockPlaceListener(BuildFFAGame game) {
        super(game.getPlugin());
        this.game = game;
    }

    private boolean permanentEdit(Player player, Block block) {
        return game.isBuildMode(player) && game.getWorldConfig() != null
                && block.getWorld().equals(game.getWorldConfig().world());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!Bukkit.isPrimaryThread()) {
            event.setCancelled(true);
            return;
        }
        Player player = event.getPlayer();
        Block block = event.getBlock();
        if (game.isFrozen(player) || game.isSpectating(player) || EventItems.isTemporary(event.getItemInHand())) {
            event.setCancelled(true);
            return;
        }
        if (game.getEventManager().isHillPlatform(block)) {
            event.setCancelled(true);
            return;
        }
        if (permanentEdit(player, block)) return;
        Gamer gamer = Gamer.getGamer(player);
        if (game.isLobbyZone(block.getLocation()) || block.getY() >= game.getWorldConfig().moveThreshold()
                || gamer == null || gamer.isVanished()
                || event.getBlockReplacedState().getType() != Material.AIR && !game.getBlockTracker().isTracked(block))
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlaced(BlockPlaceEvent event) {
        if (!Bukkit.isPrimaryThread() || game.isSpectating(event.getPlayer())) return;
        if (game.getEventManager().isHillPlatform(event.getBlock())) return;
        if (permanentEdit(event.getPlayer(), event.getBlock())) game.getBlockTracker().untrack(event.getBlock());
        else game.getBlockTracker().track(event.getBlock());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!Bukkit.isPrimaryThread()) {
            event.setCancelled(true);
            return;
        }
        Player player = event.getPlayer();
        Block block = event.getBlock();
        if (game.isFrozen(player) || game.isSpectating(player)) {
            event.setCancelled(true);
            return;
        }
        if (game.getEventManager().isHillPlatform(block)) {
            event.setCancelled(true);
            return;
        }
        if (permanentEdit(player, block)) return;
        event.setCancelled(true);
        if (block.getType() == Material.WEB && game.getBlockTracker().isTracked(block)) {
            game.getBlockTracker().untrack(block);
            block.setType(Material.AIR);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBroken(BlockBreakEvent event) {
        if (Bukkit.isPrimaryThread()) game.getBlockTracker().untrack(event.getBlock());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBuilderDamage(EntityDamageByEntityEvent event) {
        if (!Bukkit.isPrimaryThread()) return;
        if (event.getEntity() instanceof Player && game.isBuildMode((Player) event.getEntity())) event.setCancelled(true);
        if (event.getDamager() instanceof Player && game.isBuildMode((Player) event.getDamager())) event.setCancelled(true);
        if (event.getDamager() instanceof Projectile && ((Projectile) event.getDamager()).getShooter() instanceof Player
                && game.isBuildMode((Player) ((Projectile) event.getDamager()).getShooter())) event.setCancelled(true);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        if (game.getFrozenPlayers().remove(event.getPlayer().getUniqueId()) != null) {
            for (Player staff : Bukkit.getOnlinePlayers()) {
                if (game.hasPermission(staff, "buildffa.staff")) {
                    Gamer gamer = Gamer.getGamer(staff);
                    if (gamer != null) gamer.sendLocalizedMessage("buildffa_freeze_disconnect", "%player%", event.getPlayer().getName());
                }
            }
        }
        game.disableBuildMode(event.getPlayer(), false);
    }

    @EventHandler
    public void onDisable(PluginDisableEvent event) {
        if (event.getPlugin() == game.getPlugin()) { game.getFrozenPlayers().clear(); game.clearBuildModes(); }
    }
}
