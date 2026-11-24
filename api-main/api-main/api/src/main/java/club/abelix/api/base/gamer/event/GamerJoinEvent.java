package club.abelix.api.base.gamer.event;

import club.abelix.api.base.gamer.Gamer;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.event.Cancellable;

/**
 * @author Egor Skorikov (Krashe85)
 * <p>GamerJoinEvent creation on 12.09.2022 at 01:13</p>
 */
@Getter
public class GamerJoinEvent extends GamerEvent implements Cancellable {
    @Setter
    private boolean cancelled;
    @Setter
    private String cancelReason;

    public GamerJoinEvent(Gamer gamer) {
        super(true, gamer);
    }

}