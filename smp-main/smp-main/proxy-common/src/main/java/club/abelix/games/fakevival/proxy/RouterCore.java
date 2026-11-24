package club.abelix.games.fakevival.proxy;

import java.util.Optional;
import java.util.UUID;

public final class RouterCore {
    private final ProxyDatabase database;
    private final ProxyPlatformAdapter platform;
    private final ServerGroupResolver groups = new ServerGroupResolver();

    public RouterCore(ProxyPlatformAdapter platform) {
        this(new ProxyDatabase(), platform);
    }

    public RouterCore(ProxyDatabase database, ProxyPlatformAdapter platform) {
        this.database = database;
        this.platform = platform;
    }

    public Optional<ProxyDatabase.ServerInfo> select(UUID playerId, String requestedType, String dungeonId) throws Exception {
        String type = groups.normalize(requestedType);
        if (type == null) return Optional.empty();
        Optional<ProxyDatabase.ServerInfo> selected = database.select(playerId, type, platform::serverExists);
        if (selected.isEmpty()) return Optional.empty();
        ProxyDatabase.ServerInfo server = selected.get();
        database.assign(playerId, type, server.id());
        if (type.equals("DUNGEON")) {
            database.reserve(playerId, dungeonId == null || dungeonId.isBlank() ? "unknown" : dungeonId, server.id());
        }
        return selected;
    }

    public boolean route(UUID playerId, String type, String dungeonId) throws Exception {
        Optional<ProxyDatabase.ServerInfo> selected = select(playerId, type, dungeonId);
        if (selected.isEmpty()) return false;
        platform.sendPlayer(playerId, selected.get().id());
        return true;
    }

    public ProxyDatabase database() { return database; }
    public ServerGroupResolver groups() { return groups; }
}
