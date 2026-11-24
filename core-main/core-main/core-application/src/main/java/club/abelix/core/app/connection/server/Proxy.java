package club.abelix.core.app.connection.server;

import club.abelix.core.app.connection.RemoteClientConnection;
import club.abelix.core.app.data.gamer.Gamer;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Proxy extends AbstractConnectable {

    public Proxy(RemoteClientConnection connection, String name) {
        super(connection, name);
    }

    @Override
    public String toString() {
        return "Proxy/" + name;
    }

    @Override
    public boolean addPlayer(@NonNull Gamer gamer) {
        boolean added = gamers.add(gamer);

        if (added) {
            log.debug("{} connected к {}", gamer.getName(), this);
        }

        return added;
    }

    @Override
    public boolean removePlayer(@NonNull Gamer gamer) {
        boolean removed = gamers.remove(gamer);

        if (removed) {
            log.debug("{} disconnected от {}", gamer.getName(), this);
        }

        return removed;
    }
}
