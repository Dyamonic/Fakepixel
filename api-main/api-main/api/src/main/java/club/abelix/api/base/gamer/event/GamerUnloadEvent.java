package club.abelix.api.base.gamer.event;

import club.abelix.api.base.gamer.Gamer;

public class GamerUnloadEvent extends GamerEvent {

    public GamerUnloadEvent(Gamer gamer) {
        super(true, gamer);
    }
}
