package club.abelix.core.app.event.server;

import club.abelix.core.app.connection.server.AbstractServer;

/**
 * @author Egor Skorikov (Krashe85)
 * <p>Project: fakepixel-core</p>
 * <p>ServerConnectEvent creation on 26.12.2023 at 01:29</p>
 */

public class ServerConnectEvent extends ServerEvent {
    public ServerConnectEvent(AbstractServer server) {
        super(server);
    }
}
