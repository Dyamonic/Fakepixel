package club.abelix.api.base.box.animation.event;

import club.abelix.api.base.box.context.BoxOpenContext;
import club.abelix.core.bukkit.event.YEvent;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class AnimationEndEvent extends YEvent {

    BoxOpenContext context;

}
