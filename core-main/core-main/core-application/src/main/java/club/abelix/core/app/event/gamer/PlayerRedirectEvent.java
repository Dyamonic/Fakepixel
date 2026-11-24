package club.abelix.core.app.event.gamer;

import club.abelix.core.app.api.event.Cancellable;
import club.abelix.core.app.api.event.Event;
import club.abelix.core.app.connection.server.AbstractServer;
import club.abelix.core.app.data.gamer.Gamer;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class PlayerRedirectEvent extends Event implements Cancellable {

    private final Gamer gamer;

    private AbstractServer target;

    private boolean cancelled;

    public PlayerRedirectEvent(Gamer gamer, AbstractServer target) {
        this.gamer = gamer;
        this.target = target;
    }

}