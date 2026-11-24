package club.abelix.api.base.box.service;

import club.abelix.api.base.box.Box;
import club.abelix.api.base.box.animation.service.AnimationService;
import club.abelix.api.base.box.animation.state.BoxAnimationState;
import club.abelix.api.base.box.animation.type.AnimationType;
import club.abelix.api.base.box.context.BoxOpenContext;
import club.abelix.api.base.box.entity.BoxDto;
import club.abelix.api.base.box.task.TimeoutTask;
import club.abelix.api.base.box.util.RngUtil;
import club.abelix.api.base.gamer.Gamer;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import lombok.val;

@Log4j2
@RequiredArgsConstructor
public class BoxOpenService {

    private final AnimationService animationService;
    private final BoxesService boxesService;

    public void open(
            Gamer gamer,
            BoxDto boxDto,
            Box box
    ) {
        if (animationService.availableSlots() == 0) {
            gamer.sendLocalizedMessage("box_no_available_slots");
            return;
        }

        val animationType = AnimationType.getById(gamer.getProperty("Boxes/Animation/"));
        val context = new BoxOpenContext(gamer);

        context.setState(BoxAnimationState.STARTING);
        context.setBoxDto(boxDto);
        context.setBox(box);
        context.setReward(RngUtil.pick(box));

        val startLocationOptional = animationService.tryStartAnimation(
                gamer,
                animationType,
                context
        );

        if (startLocationOptional.isEmpty()) {
            gamer.sendLocalizedMessage("box_no_available_slots");
            return;
        }

        context.setOpenLocation(startLocationOptional.get());

        val task = new TimeoutTask(
                gamer,
                context,
                animationService,
                this
        ).setTickDelay(1, 20).sync().start(60);

        context.setTimeoutTask(task);

        if (boxDto != null) {
            gamer.takeBox(boxDto);
        }
    }

    public void cancel(Gamer gamer) {
        val contextOptional = animationService.getOpeningContext(gamer);
        if (contextOptional.isEmpty()) return;

        val context = contextOptional.get();
        boxesService.giveBox(gamer, context.getBoxDto());

        log.info("Cancelled box opening for gamer: {}. The box was returned", gamer.getName());
    }

}

