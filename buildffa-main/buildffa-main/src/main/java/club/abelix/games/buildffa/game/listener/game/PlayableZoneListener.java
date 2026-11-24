package club.abelix.games.buildffa.game.listener.game;

import club.abelix.api.base.listener.YListener;
import club.abelix.games.buildffa.game.BuildFFAGame;
import lombok.NonNull;
import lombok.val;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerTeleportEvent;

/**
 * @author milansky
 */
public final class PlayableZoneListener extends YListener {
    private final BuildFFAGame game;

    public PlayableZoneListener(@NonNull BuildFFAGame game) {
        super(game.getPlugin());

        this.game = game;
    }

    @EventHandler
    public void onTeleport(final @NonNull PlayerTeleportEvent event) {
        if (event.getCause() != PlayerTeleportEvent.TeleportCause.ENDER_PEARL) return;
        if (!game.isLobbyZone(event.getTo())) return;

        event.setCancelled(true);
    }
}
