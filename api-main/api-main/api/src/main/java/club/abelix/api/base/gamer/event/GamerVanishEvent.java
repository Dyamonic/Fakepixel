package club.abelix.api.base.gamer.event;

import club.abelix.api.base.gamer.Gamer;
import lombok.Getter;

@Getter
public class GamerVanishEvent extends GamerEvent {

    private final boolean vanished;

    public GamerVanishEvent(Gamer gamer, boolean vanished) {
        super(gamer);

        this.vanished = vanished;
    }
}
