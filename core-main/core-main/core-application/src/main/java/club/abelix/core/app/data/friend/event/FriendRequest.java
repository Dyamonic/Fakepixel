package club.abelix.core.app.data.friend.event;

import club.abelix.core.app.api.event.Cancellable;
import club.abelix.core.app.api.event.Event;
import club.abelix.core.app.data.gamer.Gamer;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class FriendRequest extends Event implements Cancellable {

    private final Gamer sender;
    private final Gamer gamer;
    private boolean cancelled;

    public FriendRequest(Gamer sender, Gamer gamer) {
        this.sender = sender;
        this.gamer = gamer;
    }
}
