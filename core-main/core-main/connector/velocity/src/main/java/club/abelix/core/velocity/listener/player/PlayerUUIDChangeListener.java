package club.abelix.core.velocity.listener.player;

import club.abelix.commons.data.network.NetworkSqlHandler;
import club.abelix.network.common.logging.Logging;
import com.velocitypowered.api.event.EventTask;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.GameProfileRequestEvent;
import com.velocitypowered.api.util.GameProfile;

import java.util.UUID;

/**
 * @author Egor Skorikov (Krashe85)
 * <p>PlayerUUIDChangeListener creation on 02.03.2023 at 16:26</p>
 */

public class PlayerUUIDChangeListener {

    @Subscribe(order = PostOrder.FIRST)
    public EventTask onGameProfileRequest(GameProfileRequestEvent event) {
        return EventTask.async(() -> {
            int playerId = NetworkSqlHandler.INSTANCE.getPlayerId(event.getUsername());
            if (playerId <= 0) {
                Logging.IMP.warn("User {} failed change UUID", event.getUsername());
                return;
            }

            UUID newUUID = UUID.nameUUIDFromBytes(("ID_" + playerId).getBytes());
            GameProfile newProfile = new GameProfile(newUUID, event.getOriginalProfile().getName(),
                    event.getOriginalProfile().getProperties());

            event.setGameProfile(newProfile);
            Logging.IMP.info("User {} change uuid [{} -> {}]",
                    event.getUsername(), event.getOriginalProfile().getId(), newUUID);
        });
    }

}
