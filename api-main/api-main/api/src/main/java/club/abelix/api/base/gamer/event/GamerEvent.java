package club.abelix.api.base.gamer.event;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.core.bukkit.event.YEvent;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public abstract class GamerEvent extends YEvent {

    private final Gamer gamer;

    public GamerEvent(boolean async, Gamer gamer) {
        super(async);

        this.gamer = gamer;
    }
}
