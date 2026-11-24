package club.abelix.api.base.roleplay.event;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.gamer.event.GamerEvent;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.event.Cancellable;

public class GamerSitEvent extends GamerEvent implements Cancellable {

    @Getter
    @Setter
    private boolean cancelled;

    public GamerSitEvent(Gamer gamer) {
        super(gamer);
    }
}
