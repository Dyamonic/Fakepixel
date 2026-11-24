package club.abelix.core.app.event.gamer;

import club.abelix.core.app.api.event.Cancellable;
import club.abelix.core.app.api.event.Event;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.net.InetAddress;

@Getter
@Setter
@RequiredArgsConstructor
public class PlayerLoginEvent extends Event implements Cancellable {

    private final String playerName;
    private final InetAddress address;
    private final State state;

    private boolean cancelled;

    private String kickReason;

    public enum State {
        VERIFY, POST, RECONNECT
    }

}
