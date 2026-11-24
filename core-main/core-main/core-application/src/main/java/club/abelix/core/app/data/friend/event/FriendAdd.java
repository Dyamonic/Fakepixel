package club.abelix.core.app.data.friend.event;

import club.abelix.core.app.api.event.Event;
import club.abelix.core.app.data.gamer.Gamer;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class FriendAdd extends Event {

    private final Gamer sender;
    private final Gamer gamer;

    public FriendAdd(Gamer sender, Gamer gamer) {
        this.sender = sender;
        this.gamer = gamer;
    }


}
