package club.abelix.core.app.event.proxy;

import club.abelix.core.app.api.event.Event;
import club.abelix.core.app.connection.server.Proxy;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

/**
 * @author Egor Skorikov (Krashe85)
 * <p>Project: fakepixel-core</p>
 * <p>ProxyEvent creation on 19.02.2024 at 23:27</p>
 */

@Getter
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = lombok.AccessLevel.PRIVATE)
public abstract class ProxyEvent extends Event {
    Proxy proxy;
}
