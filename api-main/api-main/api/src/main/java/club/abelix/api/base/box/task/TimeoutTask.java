package club.abelix.api.base.box.task;

import club.abelix.api.FalconAPI;
import club.abelix.api.base.box.animation.service.AnimationService;
import club.abelix.api.base.box.animation.state.BoxAnimationState;
import club.abelix.api.base.box.context.BoxOpenContext;
import club.abelix.api.base.box.service.BoxOpenService;
import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.util.task.AbstractTickingLaterTask;
import club.abelix.api.base.util.task.cancel.CancelReason;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TimeoutTask extends AbstractTickingLaterTask {

    Gamer gamer;
    BoxOpenContext context;
    AnimationService animationService;
    BoxOpenService boxOpenService;

    public TimeoutTask(Gamer gamer, BoxOpenContext context, AnimationService animationService, BoxOpenService boxOpenService) {
        super(FalconAPI.getInstance());

        this.gamer = gamer;
        this.context = context;
        this.animationService = animationService;
        this.boxOpenService = boxOpenService;
    }

    @Override
    protected void runTick() {
        gamer.sendLocalizedActionBar("box_left_time", "%time%", left);

        if (left <= 1) {
            context.setState(BoxAnimationState.CANCELED);
            cancel(CancelReason.END);
        }
    }

    @Override
    protected void handleCancel(@NonNull CancelReason reason) {
        if (reason == CancelReason.CUSTOM) return;

        boxOpenService.cancel(gamer);
        animationService.cancel(gamer);
        gamer.sendLocalizedMessage("box_time_outed_and_returned", "%boxName%", gamer.getLanguage().getMessage(context.getBox().getNameKey()));
    }
}

