package club.abelix.network.online.info;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

import java.util.concurrent.atomic.AtomicReference;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TrackedServer {

    String id;
    AtomicReference<ServerInfo> serverInfo;

    public TrackedServer(ServerInfo initial) {
        this.id = initial.getId();
        this.serverInfo = new AtomicReference<>(initial);
    }

    public void update(ServerInfo newSnapshot) {
        serverInfo.set(newSnapshot);
    }

}
