package club.abelix.core.app.event.server;


import club.abelix.core.app.connection.server.AbstractServer;

public class ServerInfoUpdateEvent extends ServerEvent {
    public ServerInfoUpdateEvent(AbstractServer server) {
        super(server);
    }
}
