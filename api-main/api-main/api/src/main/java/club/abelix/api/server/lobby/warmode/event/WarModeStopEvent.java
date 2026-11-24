package club.abelix.api.server.lobby.warmode.event;

import club.abelix.core.bukkit.event.YEvent;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.bukkit.event.Cancellable;

/**
 * @author pro0xy
 * <p>Created on 03.09.2023 at 15:49</p>
 */

@RequiredArgsConstructor
@Getter
public class WarModeStopEvent extends YEvent implements Cancellable {

    @Setter
    private boolean cancelled;
}
