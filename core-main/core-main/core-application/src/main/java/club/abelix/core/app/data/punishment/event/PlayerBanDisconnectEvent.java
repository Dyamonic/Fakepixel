package club.abelix.core.app.data.punishment.event;

import club.abelix.commons.data.punishment.meta.BanMeta;
import club.abelix.core.app.api.event.Event;
import club.abelix.core.app.data.gamer.Gamer;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@RequiredArgsConstructor
public class PlayerBanDisconnectEvent extends Event {

    private final Gamer gamer;
    private final BanMeta meta;
    private boolean disconnect = true;

}
