package club.abelix.core.app.connection.server;

import club.abelix.core.app.CoreApplication;
import club.abelix.core.app.connection.RemoteClientConnection;
import club.abelix.core.app.data.gamer.Gamer;
import club.abelix.core.app.event.server.ServerInfoUpdateEvent;
import club.abelix.network.type.mode.Master;
import club.abelix.network.type.mode.ServerMode;
import club.abelix.network.type.mode.ServerType;
import club.abelix.network.type.mode.Special;
import club.abelix.network.type.mode.Uncommon;
import club.abelix.network.type.server.ServerConnectionInfo;
import club.abelix.network.type.server.state.ServerState;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;

@Getter
@EqualsAndHashCode(callSuper = true, exclude = {"slots", "serverState", "type"})
public abstract class AbstractServer extends AbstractConnectable {

    protected final ServerType type;
    protected volatile int slots;
    protected volatile ServerState serverState = ServerState.WAITING_POD;

    public void setSlots(int slots) {
        withLock(() -> this.slots = slots);
    }

    public void setServerState(ServerState serverState) {
        withLock(() -> this.serverState = serverState);
    }

    public AbstractServer(RemoteClientConnection connection, ServerType type) {
        super(connection, type.getServerName());

        this.type = type;
    }

    public boolean isSpecial() {
        return type.getMode() instanceof Special;
    }

    public String getPrefix() {
        return type.getPrefix();
    }

    public int getServerNumber() {
        return type.getServerNumber();
    }

    public int getGameNumber() {
        return type.getGameNumber();
    }

    public Uncommon getUncommon() {
        return type.getUncommon();
    }

    public ServerMode getMode() {
        return type.getMode();
    }

    public Master getMaster() {
        return type.getMaster();
    }

    public boolean canJoin() {
        return serverState == ServerState.RUNNING && !isUnhealthy();
    }

    public int getSpace() {
        return this.slots - this.getGamers().size();
    }

    public ServerConnectionInfo getInfo() {
        return new ServerConnectionInfo(name, getHost(), getPort());
    }

    public boolean isAddingToProxy() {
        return true;
    }

    public double getFillRate() {
        if (this.slots == 0) {
            return 1.0;
        }

        return (double) this.getGamers().size() / this.slots;
    }

    public String getHost() {
        return connection.getHost();
    }

    public int getPort() {
        return connection.getPort();
    }

    @Override
    public boolean addPlayer(@NonNull Gamer gamer) {
        boolean added = gamers.add(gamer);

        if (added) {
            CoreApplication.getInstance().getPluginManager().callEvent(new ServerInfoUpdateEvent(this));
        }

        return added;
    }

    @Override
    public boolean removePlayer(@NonNull Gamer gamer) {
        boolean removed = gamers.remove(gamer);

        if (removed) {
            CoreApplication.getInstance().getPluginManager().callEvent(new ServerInfoUpdateEvent(this));
        }

        return removed;
    }

    @Override
    public String toString() {
        return "AbstractServer/" + getName();
    }

}
