package club.abelix.core.app.event.proxy;

import club.abelix.core.app.connection.server.Proxy;

/**
 * @author Egor Skorikov (Krashe85)
 * <p>Project: fakepixel-core</p>
 * <p>ProxyDisconnectEvent creation on 19.02.2024 at 23:27</p>
 */

public class ProxyDisconnectEvent extends ProxyEvent {
    public ProxyDisconnectEvent(Proxy proxy) {
        super(proxy);
    }
}
