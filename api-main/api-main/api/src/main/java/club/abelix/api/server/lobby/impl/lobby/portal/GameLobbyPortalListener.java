package club.abelix.api.server.lobby.impl.lobby.portal;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.util.core.CoreUtil;
import club.abelix.api.server.lobby.portal.AbstractPortalListener;
import club.abelix.network.type.connect.ConnectStatus;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public class GameLobbyPortalListener extends AbstractPortalListener {

    @Override
    protected CompletableFuture<ConnectStatus> connect(@NotNull Player player) {
        Gamer gamer = Gamer.getGamer(player);

        return gamer == null
                ? CompletableFuture.completedFuture(ConnectStatus.GAMER_NOT_FOUND)
                : gamer.connectMode(CoreUtil.getMaster(), true);
    }
}
