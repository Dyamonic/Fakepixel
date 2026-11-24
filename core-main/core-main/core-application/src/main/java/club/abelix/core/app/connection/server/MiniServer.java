package club.abelix.core.app.connection.server;

import club.abelix.core.app.connection.RemoteClientConnection;
import club.abelix.network.type.mode.Master;
import club.abelix.network.type.mode.ServerType;
import club.abelix.network.type.server.state.ServerState;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@EqualsAndHashCode(callSuper = true, exclude = {"masterServer"})
public class MiniServer extends AbstractServer {

    @Getter
    private final MasterServer masterServer;

    public MiniServer(MasterServer masterServer, RemoteClientConnection connection, ServerType type) {
        super(connection, type);

        this.masterServer = masterServer;
    }

    @Override
    public Master getMaster() {
        return masterServer.getMaster();
    }

    @Override
    public ServerState getServerState() {
        return getMasterServer().getServerState();
    }

    @Override
    public boolean canJoin() {
        return masterServer.canJoin();
    }
}
