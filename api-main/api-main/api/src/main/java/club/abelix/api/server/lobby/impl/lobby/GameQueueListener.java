package club.abelix.api.server.lobby.impl.lobby;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.hotbar.manager.HotbarManager;
import club.abelix.api.base.listener.YListener;
import club.abelix.core.bukkit.HeartBeat;
import club.abelix.core.bukkit.event.ServerActiveEvent;
import club.abelix.core.bukkit.event.ServerInactiveEvent;
import club.abelix.network.packet.ChannelProcessorContext;
import club.abelix.network.packet.play.both.BQueueJoinQueue;
import club.abelix.network.packet.play.processor.PlayProcessor;
import lombok.NonNull;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.metadata.FixedMetadataValue;

/**
 * @author WhileInside
 */
public class GameQueueListener extends YListener implements PlayProcessor {

    private final GameLobbyServer lobby;

    public GameQueueListener(@NonNull GameLobbyServer lobby) {
        super(lobby.getPlugin());

        this.lobby = lobby;
    }

    @Override
    public void process(BQueueJoinQueue packet, ChannelProcessorContext ctx) {
        // участника пати кинуло в очередь
        Gamer gamer = Gamer.getGamer(packet.getPlayerId());

        if (gamer == null) {
            return;
        }

        Player player = gamer.getPlayer();
        player.setMetadata("queued", new FixedMetadataValue(lobby.getPlugin(), Boolean.TRUE));

        HotbarManager.INSTANCE.updateIcons(gamer);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        event.getPlayer().removeMetadata("queued", lobby.getPlugin());
    }

    @EventHandler
    public void onInactive(ServerInactiveEvent event) {
        HeartBeat.runSync(() -> {
            for (Gamer gamer : Gamer.getGamers()) {
                Player player = gamer.getPlayer();

                player.removeMetadata("queued", lobby.getPlugin());
//                gamer.sendLocalizedMessage("queue_removed_by_core");

                HotbarManager.INSTANCE.updateIcons(gamer);
            }
        });
    }

    @EventHandler
    public void onActive(ServerActiveEvent event) {
        event.getProcessor().registerProcessor(this);
    }
}
