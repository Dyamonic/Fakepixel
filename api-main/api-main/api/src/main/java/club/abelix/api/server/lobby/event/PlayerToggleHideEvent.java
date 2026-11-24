package club.abelix.api.server.lobby.event;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.gamer.event.GamerEvent;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.bukkit.event.Cancellable;

@Getter
public class PlayerToggleHideEvent extends GamerEvent implements Cancellable {

    private final Gamer target;
    private final boolean hiding;

    @Setter
    private boolean cancelled;

    public PlayerToggleHideEvent(@NonNull Gamer gamer, @NonNull Gamer target, boolean hiding) {
        super(gamer);

        this.target = target;
        this.hiding = hiding;
    }
}
