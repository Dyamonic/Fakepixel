package club.abelix.core.app.event.server;

import club.abelix.core.app.api.event.Event;
import club.abelix.core.app.connection.server.AbstractServer;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * @author Egor Skorikov (Krashe85)
 * <p>Project: fakepixel-core</p>
 * <p>ServerEvent creation on 26.12.2023 at 01:30</p>
 */

@Getter
@RequiredArgsConstructor
public class ServerEvent extends Event {
    private final AbstractServer server;
}
