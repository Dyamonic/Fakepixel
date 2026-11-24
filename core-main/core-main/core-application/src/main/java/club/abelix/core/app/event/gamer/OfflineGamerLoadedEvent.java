package club.abelix.core.app.event.gamer;

import club.abelix.core.app.data.gamer.Gamer;

public class OfflineGamerLoadedEvent extends GamerEvent {

    public OfflineGamerLoadedEvent(Gamer gamer) {
        super(gamer);
    }
}
