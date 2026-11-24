package club.abelix.core.app.event.gamer;

import club.abelix.core.app.api.event.Event;
import club.abelix.core.app.data.gamer.Gamer;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;

/**
 * Created by saharNooby on 19:01 27.09.2017
 */
@EqualsAndHashCode(callSuper = true)
@Data
@RequiredArgsConstructor
public class PlayerQuitEvent extends Event {

    private final Gamer gamer;

}
