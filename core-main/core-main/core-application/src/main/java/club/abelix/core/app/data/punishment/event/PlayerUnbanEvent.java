package club.abelix.core.app.data.punishment.event;

import club.abelix.core.app.api.event.Event;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
public final class PlayerUnbanEvent extends Event {

    String name;

}
