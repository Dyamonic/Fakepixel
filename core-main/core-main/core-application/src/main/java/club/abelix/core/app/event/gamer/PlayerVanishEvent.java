package club.abelix.core.app.event.gamer;

import club.abelix.core.app.api.event.Event;
import club.abelix.core.app.data.gamer.Gamer;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class PlayerVanishEvent extends Event {

    private final Gamer gamer;

    private final boolean vanished;

}
