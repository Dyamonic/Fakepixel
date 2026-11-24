package club.abelix.api.base.box.animation.listener;

import club.abelix.api.base.box.animation.event.AnimationChoseEvent;
import club.abelix.api.base.box.animation.event.AnimationEndEvent;
import club.abelix.api.base.box.animation.service.AnimationService;
import club.abelix.api.base.box.animation.state.BoxAnimationState;
import club.abelix.api.base.box.service.BoxAuditHook;
import club.abelix.api.base.gamer.Gamer;
import club.abelix.commons.data.cases.rarity.Rarity;
import club.abelix.core.bukkit.HeartBeat;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.val;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class AnimationListener implements Listener {

    private static final int ANIMATION_FINISH_DELAY_TICKS = 20 * 10;
    AnimationService animationService;

    @EventHandler
    public void onAnimationEnd(AnimationEndEvent event) {
        event.getContext().setState(BoxAnimationState.WAITING_FOR_CHOSE);
    }

    @EventHandler
    public void onAnimationChose(AnimationChoseEvent event) {
        val context = event.getContext();
        val gamer = context.getGamer();
        val reward = context.getReward();
        val language = gamer.getLanguage();

        reward.sendCustomRewardMessage(gamer);

        boolean applied = reward.apply(context.getGamer());

        if (!applied) {
            val equivalentType = reward.getEquivalentType();
            val equivalent = reward.getEquivalent(gamer);

            equivalentType.giveDuplicateEquivalent(gamer, equivalent);
            equivalentType.sendDuplicateEquivalentMessage(gamer, equivalent);
        }

        val boxDto = context.getBoxDto();

        BoxAuditHook.logOpen(
                gamer,
                context.getBox(),
                reward,
                applied,
                boxDto == null ? null : boxDto.getServerMode()
        );

        context.getAnimation().revealRewards();
        context.getTimeoutTask().cancel();

        HeartBeat.runLaterSync(() -> {
            animationService.finishAnimation(context);
            gamer.getPlayer().teleport(context.getInitialPlayerLocation());
        }, ANIMATION_FINISH_DELAY_TICKS);

        gamer.sendLocalizedMessage("box_opened_reward_self",
                "%reward%", reward.getName(language),
                "%rarity%", reward.getRarity().getName(language),
                "%player%", gamer.getDisplayName()
        );

        gamer.sendTitle(
                language.getMessage("box_opened_reward_title",
                        "%reward%", reward.getName(language),
                        "%rarity%", reward.getRarity().getName(language),
                        "%player%", gamer.getDisplayName()
                ),
                language.getMessage("box_opened_reward_subtitle",
                        "%reward%", reward.getName(language),
                        "%rarity%", reward.getRarity().getName(language),
                        "%player%", gamer.getDisplayName()
                )
        );

        if (reward.getRarity().equals(Rarity.RARE) || reward.getRarity().equals(Rarity.EPIC) || reward.getRarity().equals(Rarity.LEGENDARY)) {
            Gamer.getGamers().forEach(g -> {
                if (!g.equals(gamer)) {
                    g.sendLocalizedMessage("box_opened_reward",
                            "%reward%", reward.getName(g.getLanguage()),
                            "%rarity%", reward.getRarity().getName(g.getLanguage()),
                            "%player%", gamer.getDisplayName(),
                            "%boxName%", g.getLanguage().getMessage(context.getBox().getNameKey())
                    );
                }

            });
        }
    }

}
