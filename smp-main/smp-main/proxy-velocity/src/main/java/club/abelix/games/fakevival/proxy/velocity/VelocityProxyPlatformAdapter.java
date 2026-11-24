package club.abelix.games.fakevival.proxy.velocity;

import club.abelix.games.fakevival.proxy.ProxyPlatformAdapter;
import com.velocitypowered.api.proxy.ProxyServer;

import java.util.UUID;

public final class VelocityProxyPlatformAdapter implements ProxyPlatformAdapter {
    private final ProxyServer proxy;

    public VelocityProxyPlatformAdapter(ProxyServer proxy) { this.proxy = proxy; }

    @Override public void sendPlayer(UUID playerId, String serverId) {
        proxy.getPlayer(playerId).ifPresent(player -> proxy.getServer(serverId)
            .ifPresent(server -> player.createConnectionRequest(server).fireAndForget()));
    }
    @Override public boolean isPlayerOnline(UUID playerId) { return proxy.getPlayer(playerId).isPresent(); }
    @Override public int getOnlineCount(String serverId) { return proxy.getServer(serverId).map(s -> s.getPlayersConnected().size()).orElse(0); }
    @Override public boolean serverExists(String serverId) { return proxy.getServer(serverId).isPresent(); }
}
