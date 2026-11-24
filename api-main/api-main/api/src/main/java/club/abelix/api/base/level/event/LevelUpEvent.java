package club.abelix.api.base.level.event;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.core.bukkit.event.YEvent;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class LevelUpEvent extends YEvent {

    private final Gamer gamer;

}
