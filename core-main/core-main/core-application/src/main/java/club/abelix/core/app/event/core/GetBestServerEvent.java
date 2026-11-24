package club.abelix.core.app.event.core;

import club.abelix.core.app.api.event.Event;
import club.abelix.core.app.connection.server.AbstractServer;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.net.InetAddress;

/**
 * @author artem
 */
@Setter
@Getter
@RequiredArgsConstructor
public class GetBestServerEvent extends Event {

    private final String name;
    private final InetAddress address;

    private AbstractServer bestServer;

}
