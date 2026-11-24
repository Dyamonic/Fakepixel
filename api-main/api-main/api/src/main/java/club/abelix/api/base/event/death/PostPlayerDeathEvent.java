package club.abelix.api.base.event.death;

import club.abelix.core.bukkit.event.YEvent;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;

@RequiredArgsConstructor
public class PostPlayerDeathEvent extends YEvent {

    @Getter
    private final Player player;

    @Getter
    private final EntityDamageEvent.DamageCause lastCause;
}