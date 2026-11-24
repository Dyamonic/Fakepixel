package club.abelix.core.app.connection.server;

import club.abelix.core.app.connection.RemoteClientConnection;
import club.abelix.network.type.mode.ServerType;
import lombok.EqualsAndHashCode;
import lombok.Setter;


@EqualsAndHashCode(callSuper = true, exclude = {"joinable"})
public class CommonServer extends AbstractServer {
    protected volatile boolean joinable;

    public void setJoinable(boolean joinable) {
        withLock(() -> this.joinable = joinable);
    }

    public CommonServer(RemoteClientConnection connection, ServerType type) {
        super(connection, type);
    }

    @Override
    public boolean canJoin() {
        return joinable && super.canJoin();
    }

    @Override
    public String toString() {
        return "Common/" + name;
    }
}
