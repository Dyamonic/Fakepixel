package club.abelix.core.app.data.punishment.event;

import club.abelix.core.app.api.event.Event;
import club.abelix.core.app.data.gamer.Gamer;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/**
 * @author Egor Skorikov (Krashe85)
 * <p>Project: core</p>
 * <p>PlayerBanEvent creation on 02.04.2023 at 15:52</p>
 */

@Getter
@Setter
@RequiredArgsConstructor
public class PlayerBanEvent extends Event {
    private final Gamer gamer;
    private final long time;
    private final boolean permanent;
}
