package club.abelix.core.app.event.core;

import club.abelix.core.app.api.event.Event;
import club.abelix.core.app.connection.RemoteClientConnection;
import club.abelix.network.channel.AbstractRemoteClientChannel;
import club.abelix.network.packet.BossProcessor;
import lombok.Getter;

import java.util.Optional;

/**
 * @author WhileInside
 */
public class ClientActiveEvent extends Event {

    private RemoteClientConnection clientChannel = null;

    @Getter
    private final BossProcessor processor;

    public ClientActiveEvent(BossProcessor processor) {
        this.processor = processor;
    }

    public ClientActiveEvent(BossProcessor processor, RemoteClientConnection clientChannel) {
        this.processor = processor;
        this.clientChannel = clientChannel;
    }

    public Optional<RemoteClientConnection> getClientChannel() {
        return Optional.ofNullable(clientChannel);
    }

}
