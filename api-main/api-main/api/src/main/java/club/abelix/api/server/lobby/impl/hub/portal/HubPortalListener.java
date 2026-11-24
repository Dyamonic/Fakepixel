package club.abelix.api.server.lobby.impl.hub.portal;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.server.lobby.portal.AbstractPortalListener;
import club.abelix.commons.util.random.RandomUtil;
import club.abelix.network.type.connect.ConnectStatus;
import club.abelix.network.type.mode.Master;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public class HubPortalListener extends AbstractPortalListener {

    @Override
    protected CompletableFuture<ConnectStatus> connect(@NotNull Player player) {
        Gamer gamer = Gamer.getGamer(player);
        return gamer.connectMode(RandomUtil.getRandomElement(Master.VALUES), true);
    }
}
