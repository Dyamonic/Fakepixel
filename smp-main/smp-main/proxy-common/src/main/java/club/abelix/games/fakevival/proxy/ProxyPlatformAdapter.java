package club.abelix.games.fakevival.proxy;

import java.util.UUID;

public interface ProxyPlatformAdapter {
    void sendPlayer(UUID playerId, String serverId);
    boolean isPlayerOnline(UUID playerId);
    int getOnlineCount(String serverId);
    boolean serverExists(String serverId);
}
