package club.abelix.api.base.gamer.event;

import club.abelix.api.base.gamer.Gamer;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.event.Cancellable;

import java.util.UUID;

@Getter
public class GamerAsyncLoadEvent extends GamerEvent implements Cancellable {

    private final UUID uniqueId;
    @Setter
    private boolean cancelled;
    @Setter
    private String cancelReason;

    public GamerAsyncLoadEvent(Gamer gamer, UUID uniqueId) {
        super(true, gamer);
        this.uniqueId = uniqueId;
    }

}
