package club.abelix.api.base.division.status.event;

import club.abelix.api.base.division.status.Status;
import club.abelix.api.base.gamer.Gamer;
import club.abelix.core.bukkit.event.YEvent;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.bukkit.event.Cancellable;

/**
 * @author pro0xy
 * <p>Created on 04.09.2023 at 22:29</p>
 */

@RequiredArgsConstructor
@Getter
public class UpdateGamerStatusEvent extends YEvent implements Cancellable {

    private final Gamer gamer;

    private final Status status;

    @Setter
    private boolean cancelled;
}
