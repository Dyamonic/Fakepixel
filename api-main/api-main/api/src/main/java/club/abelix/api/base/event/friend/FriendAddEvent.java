package club.abelix.api.base.event.friend;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.core.bukkit.event.YEvent;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class FriendAddEvent extends YEvent {

    private final Gamer gamer;
    private final int friendId;

    public FriendAddEvent(Gamer gamer, int friendId) {
        this.gamer = gamer;
        this.friendId = friendId;
    }
}
