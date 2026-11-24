package club.abelix.api.base.gamer.event;

import club.abelix.api.base.gamer.Gamer;

public class GamerDisplayNameUpdateEvent extends GamerEvent {

    public GamerDisplayNameUpdateEvent(Gamer gamer) {
        super(gamer);
    }

}
