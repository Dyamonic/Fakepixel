package club.abelix.core.app.event.server;

import club.abelix.core.app.connection.server.AbstractServer;

/**
 * @author Egor Skorikov (Krashe85)
 * <p>Project: fakepixel-core</p>
 * <p>ServerDisconnectEvent creation on 26.12.2023 at 01:37</p>
 */

public class ServerDisconnectEvent extends ServerEvent {
    public ServerDisconnectEvent(AbstractServer server) {
        super(server);
    }
}
