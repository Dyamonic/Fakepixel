package club.abelix.games.fakevival.proxy.bungee;

import club.abelix.games.fakevival.proxy.ProxyPlatformAdapter;
import net.md_5.bungee.api.ProxyServer;

import java.util.UUID;

public final class BungeeProxyPlatformAdapter implements ProxyPlatformAdapter {
    private final ProxyServer proxy;

    public BungeeProxyPlatformAdapter(ProxyServer proxy) { this.proxy = proxy; }

    @Override public void sendPlayer(UUID playerId, String serverId) {
        var player = proxy.getPlayer(playerId);
        var server = proxy.getServerInfo(serverId);
        if (player != null && server != null) player.connect(server);
    }
    @Override public boolean isPlayerOnline(UUID playerId) { return proxy.getPlayer(playerId) != null; }
    @Override public int getOnlineCount(String serverId) {
        var server = proxy.getServerInfo(serverId);
        return server == null ? 0 : server.getPlayers().size();
    }
    @Override public boolean serverExists(String serverId) { return proxy.getServerInfo(serverId) != null; }
}
