package club.abelix.core.app.event.gamer;

import club.abelix.core.app.api.event.Cancellable;
import club.abelix.core.app.data.gamer.Gamer;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class GamerLoadEvent extends GamerEvent implements Cancellable {

    private boolean cancelled;
    private String cancelReason;

    public GamerLoadEvent(Gamer gamer) {
        super(gamer);
    }

}
