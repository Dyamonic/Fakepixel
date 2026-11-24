package club.abelix.api.server.lobby.manager;

import club.abelix.api.base.util.online.ServerOnline;
import club.abelix.api.server.lobby.BaseLobbyServer;
import lombok.NonNull;

/**
 * @author WhileInside
 */
public class LobbyManager {

    public static final LobbyManager INSTANCE = new LobbyManager();

    private BaseLobbyServer server;

    @SuppressWarnings("unchecked")
    public <T extends BaseLobbyServer> T getLobby() {
        return server == null ? null : (T) server;
    }

    public void setLobby(@NonNull BaseLobbyServer server) {
        this.server = server;
        this.server.loadLobby();
    }

    public int getGlobalOnline() {
        return ServerOnline.getOnline();
    }

}
