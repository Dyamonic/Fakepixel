package club.abelix.api.server.base.language.event;

import club.abelix.api.server.base.language.LocalizableObject;
import club.abelix.core.bukkit.event.YEvent;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class LocalizableObjectCreateEvent extends YEvent {

    private final LocalizableObject object;

}
