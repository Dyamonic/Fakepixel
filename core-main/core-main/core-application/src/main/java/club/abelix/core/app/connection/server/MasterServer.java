package club.abelix.core.app.connection.server;

import club.abelix.core.app.connection.RemoteClientConnection;
import club.abelix.core.app.data.gamer.Gamer;
import club.abelix.network.type.mode.ServerType;
import club.abelix.network.type.server.state.ServerState;
import lombok.EqualsAndHashCode;
import lombok.NonNull;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@EqualsAndHashCode(callSuper = true, exclude = {"servers"})
public class MasterServer extends CommonServer {
    private final Map<String, MiniServer> servers = new ConcurrentHashMap<>();

    public MasterServer(RemoteClientConnection connection, ServerType type) {
        super(connection, type);
    }

    public void addServer(MiniServer server) {
        synchronized (servers) {
            this.servers.put(server.getName().toLowerCase(), server);

            ServerManager.IMP.addServer(server);
        }
    }

    @Override
    public boolean isAddingToProxy() {
        return false;
    }

    @Override
    public boolean addPlayer(@NonNull Gamer gamer) {
        return false;
    }

    @Override
    public boolean removePlayer(@NonNull Gamer gamer) {
        return false;
    }

    public Set<String> removeServers() {
        synchronized (servers) {
            Set<String> servers = new HashSet<>(this.servers.keySet());

            this.servers.values().forEach(ServerManager.IMP::removeServer);
            this.servers.clear();

            return servers;
        }
    }

    public Collection<MiniServer> getServers() {
        return Collections.unmodifiableCollection(servers.values());
    }

    public MiniServer getServer(String name) {
        return servers.get(name.toLowerCase());
    }

    @Override
    public String toString() {
        return "Master/" + name;
    }
}
