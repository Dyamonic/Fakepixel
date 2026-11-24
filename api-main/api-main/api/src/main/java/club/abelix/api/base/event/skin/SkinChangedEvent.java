package club.abelix.api.base.event.skin;

import org.bukkit.entity.Player;

public final class SkinChangedEvent extends SkinEvent {

    public SkinChangedEvent(Player player, String value, String signature) {
        super(player, value, signature);
    }
}
