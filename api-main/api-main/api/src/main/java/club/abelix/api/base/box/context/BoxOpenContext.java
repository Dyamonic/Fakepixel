package club.abelix.api.base.box.context;

import club.abelix.api.base.box.Box;
import club.abelix.api.base.box.animation.BoxOpenAnimation;
import club.abelix.api.base.box.animation.state.BoxAnimationState;
import club.abelix.api.base.box.entity.BoxDto;
import club.abelix.api.base.box.reward.BoxReward;
import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.util.task.AbstractTickingLaterTask;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.bukkit.Location;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
@RequiredArgsConstructor
public class BoxOpenContext {

    final Gamer gamer;

    BoxOpenAnimation animation;
    BoxDto boxDto;
    Box box;
    BoxAnimationState state;
    Location openLocation;
    Location initialPlayerLocation;
    AbstractTickingLaterTask timeoutTask;
    BoxReward reward;

}
