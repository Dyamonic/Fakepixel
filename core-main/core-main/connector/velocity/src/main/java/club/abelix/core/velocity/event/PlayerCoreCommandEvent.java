package club.abelix.core.velocity.event;

import com.velocitypowered.api.proxy.Player;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/**
 * Вызывается когда игрок выполняет какую-то команду от кора
 */
@RequiredArgsConstructor
@Getter
public class PlayerCoreCommandEvent {

    private final Player player;
    private final String command;

    @Setter
    private boolean cancelled;

}
