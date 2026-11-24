package club.abelix.core.bukkit.health;

import club.abelix.core.bukkit.event.YEvent;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class HealthRestartRequestedEvent extends YEvent {

    int secondsUntilRestart;

}
