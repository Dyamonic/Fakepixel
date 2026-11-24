package club.abelix.api.base.gamer.event;

import club.abelix.api.base.gamer.Gamer;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.event.Cancellable;

@Getter
public class GamerPropertyChangeEvent extends GamerEvent implements Cancellable {

    private final String property;

    private final int oldValue, newValue;

    @Setter
    private boolean cancelled;

    public GamerPropertyChangeEvent(Gamer gamer, String property, int oldValue, int newValue) {
        super(gamer);

        this.property = property;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }
}
