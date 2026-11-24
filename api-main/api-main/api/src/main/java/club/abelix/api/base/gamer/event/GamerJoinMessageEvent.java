package club.abelix.api.base.gamer.event;

import club.abelix.api.base.gamer.Gamer;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.event.Cancellable;

@Getter
public class GamerJoinMessageEvent extends GamerEvent implements Cancellable {

    @Setter
    private boolean cancelled;

    public GamerJoinMessageEvent(Gamer gamer) {
        super(gamer);
    }

}
