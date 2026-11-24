package club.abelix.games.buildffa.game.listener.game;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.listener.YListener;
import club.abelix.games.buildffa.game.BuildFFAGame;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.util.Vector;

import java.util.UUID;

public class PlayerMoveListener extends YListener implements Listener {
    private final BuildFFAGame game;
    private UUID correctingPlayer;

    public PlayerMoveListener(BuildFFAGame game) {
        super(game.getPlugin());
        this.game = game;
        Bukkit.getScheduler().runTaskTimer(game.getPlugin(), this::holdFrozenPlayers, 1L, 1L);
    }

    private boolean samePosition(Location first, Location second) {
        return first.getWorld().equals(second.getWorld())
                && first.getX() == second.getX() && first.getY() == second.getY() && first.getZ() == second.getZ();
    }

    private Location lookingAt(Location anchor, Location look) {
        Location destination = anchor.clone();
        destination.setYaw(look.getYaw());
        destination.setPitch(look.getPitch());
        return destination;
    }

    private void holdFrozenPlayers() {
        game.getFrozenPlayers().forEach((id, anchor) -> {
            Player player = Bukkit.getPlayer(id);
            if (player == null || !player.isOnline()) return;
            if (player.isInsideVehicle()) player.leaveVehicle();
            if (player.getVelocity().lengthSquared() != 0) player.setVelocity(new Vector());
            Location current = player.getLocation();
            if (samePosition(current, anchor)) return;
            correctingPlayer = id;
            try {
                player.teleport(lookingAt(anchor, current));
            } finally {
                correctingPlayer = null;
            }
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onFrozenMove(PlayerMoveEvent event) {
        if (event instanceof PlayerTeleportEvent) return;
        Location anchor = game.getFrozenPlayers().get(event.getPlayer().getUniqueId());
        Location to = event.getTo();
        if (anchor == null || to == null || samePosition(anchor, to)) return;
        event.setFrom(lookingAt(anchor, to));
        event.setCancelled(true);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (game.isFrozen(player)) return;
        Gamer gamer = Gamer.getGamer(player);
        int yThreshold = game.getWorldConfig().moveThreshold();
        if (game.isSpectating(player) || game.isBuildMode(player) || gamer.isVanished()) return;
        if (player.getLocation().getY() <= yThreshold) {
            if (!game.getPlayersAppliedKits().contains(player)) game.applySelectedKit(gamer);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onFrozenTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        if (game.isFrozen(player) && !player.getUniqueId().equals(correctingPlayer)) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onFrozenVehicleEnter(VehicleEnterEvent event) {
        if (event.getEntered() instanceof Player && game.isFrozen((Player) event.getEntered())) event.setCancelled(true);
    }
}