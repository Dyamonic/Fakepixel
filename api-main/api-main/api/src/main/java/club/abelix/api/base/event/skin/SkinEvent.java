package club.abelix.api.base.event.skin;

import club.abelix.core.bukkit.event.YEvent;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Player;

@Getter
@RequiredArgsConstructor
public abstract class SkinEvent extends YEvent {

    private final Player player;

    private final String value, signature;

}
