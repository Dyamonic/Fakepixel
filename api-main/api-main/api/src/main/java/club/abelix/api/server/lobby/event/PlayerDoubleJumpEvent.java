package club.abelix.api.server.lobby.event;

import club.abelix.core.bukkit.event.YEvent;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;

@RequiredArgsConstructor
@Getter
public class PlayerDoubleJumpEvent extends YEvent implements Cancellable {

    private final Player player;

    @Setter
    private boolean cancelled;

}
