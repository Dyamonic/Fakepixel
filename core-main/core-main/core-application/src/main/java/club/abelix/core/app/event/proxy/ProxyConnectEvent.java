package club.abelix.core.app.event.proxy;

import club.abelix.core.app.connection.server.Proxy;

/**
 * @author Egor Skorikov (Krashe85)
 * <p>Project: fakepixel-core</p>
 * <p>ProxyConnectEvent creation on 19.02.2024 at 23:27</p>
 */

public class ProxyConnectEvent extends ProxyEvent {
    public ProxyConnectEvent(Proxy proxy) {
        super(proxy);
    }
}
