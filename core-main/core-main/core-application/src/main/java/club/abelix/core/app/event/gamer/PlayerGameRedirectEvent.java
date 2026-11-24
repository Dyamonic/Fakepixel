package club.abelix.core.app.event.gamer;

import club.abelix.core.app.api.event.Event;
import club.abelix.core.app.data.gamer.Gamer;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;

import java.util.HashSet;
import java.util.Set;

@EqualsAndHashCode(callSuper = true)
@Data
@RequiredArgsConstructor
public class PlayerGameRedirectEvent extends Event {

    private final Gamer gamer;

    private final Set<Gamer> additionalGamerList = new HashSet<>();

}
