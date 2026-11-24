package club.abelix.games.fastbridge.game.events;

import club.abelix.core.bukkit.event.YEvent;
import club.abelix.games.fastbridge.commons.FastBridgeGamer;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class FastBridgeGamerRecordEvent extends YEvent {

    private final FastBridgeGamer gamer;

}
