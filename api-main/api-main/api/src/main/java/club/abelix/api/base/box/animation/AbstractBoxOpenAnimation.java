package club.abelix.api.base.box.animation;

import club.abelix.api.base.box.context.BoxOpenContext;
import club.abelix.api.base.gamer.Gamer;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public abstract class AbstractBoxOpenAnimation implements BoxOpenAnimation {

    BoxOpenContext context;
    Gamer gamer;

}
