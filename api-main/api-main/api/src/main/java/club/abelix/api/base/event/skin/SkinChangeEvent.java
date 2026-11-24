package club.abelix.api.base.event.skin;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;

@Setter
@Getter
public final class SkinChangeEvent extends SkinEvent implements Cancellable {

    private boolean cancelled;

    public SkinChangeEvent(Player player, String value, String signature) {
        super(player, value, signature);
    }

}
