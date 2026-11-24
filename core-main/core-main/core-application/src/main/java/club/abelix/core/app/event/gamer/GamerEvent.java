package club.abelix.core.app.event.gamer;

import club.abelix.core.app.api.event.Event;
import club.abelix.core.app.data.gamer.Gamer;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public abstract class GamerEvent extends Event {

    private final Gamer gamer;

}
