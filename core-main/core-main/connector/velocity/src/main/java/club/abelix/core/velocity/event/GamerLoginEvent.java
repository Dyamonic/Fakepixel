package club.abelix.core.velocity.event;

import club.abelix.core.velocity.gamer.Gamer;
import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.annotation.AwaitingEvent;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;


@Getter
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AwaitingEvent
public final class GamerLoginEvent implements ResultedEvent<ResultedEvent.ComponentResult> {

    Gamer gamer;

    @Setter
    @NonFinal
    ComponentResult result = ComponentResult.allowed(); // default = allowed

}
