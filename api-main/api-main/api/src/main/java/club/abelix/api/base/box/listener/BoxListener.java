package club.abelix.api.base.box.listener;

import club.abelix.api.base.box.animation.service.AnimationService;
import club.abelix.api.base.box.animation.state.BoxAnimationState;
import club.abelix.api.base.box.service.BoxOpenService;
import club.abelix.api.base.box.service.BoxesService;
import club.abelix.api.base.box.type.StandardBoxType;
import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.gamer.event.GamerJoinEvent;
import club.abelix.commons.data.cases.rarity.Rarity;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.log4j.Log4j2;
import lombok.val;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.time.LocalDate;

@Log4j2
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class BoxListener implements Listener {

    AnimationService animationService;
    BoxOpenService openService;
    BoxesService boxesService;

    @EventHandler
    public void onInteractInAnimation(final PlayerInteractEvent event) {
        if (event.getClickedBlock() == null) return;

        val gamer = Gamer.getGamer(event.getPlayer());

        if (!animationService.isOpening(gamer)) return;

        val contextOptional = animationService.getOpeningContext(gamer);

        if (contextOptional.isEmpty()) return;

        val context = contextOptional.get();

        if (context.getState() != BoxAnimationState.WAITING_FOR_CHOSE) return;

        context.getAnimation().onInteractEvent(
                event.getClickedBlock().getLocation()
        );
    }

    @EventHandler
    public void onPlayerQuit(final PlayerQuitEvent event) {
        val gamer = Gamer.getGamer(event.getPlayer());
        val contextOptional = animationService.getOpeningContext(gamer);

        if (contextOptional.isEmpty()) return;

        val context = contextOptional.get();
        val state = context.getState();

        if (state == BoxAnimationState.REWARD_CHOSEN) {
            log.info("Player {} disconnected during box animation, but already chose reward, finishing animation.",
                    gamer.getName()
            );
            animationService.finishAnimation(context);
            return;
        }

        log.info("Player {} disconnected during box animation in state {}, cancelling box opening.",
                gamer.getName(),
                state
        );

        openService.cancel(gamer);
        animationService.finishAnimation(context);
    }

    @EventHandler
    public void onSpecialDate(final GamerJoinEvent event) {
        LocalDate targetDate = LocalDate.of(2025, 9, 6);

        LocalDate today = LocalDate.now();

        if (!today.equals(targetDate)) return;

        val gamer = event.getGamer();

        if (gamer.getProperty("Boxes/Free") == 1) return;

        val boxType = StandardBoxType.LOBBY_COSMETIC;

        boxesService.giveBox(
                gamer,
                Rarity.RARE,
                null,
                boxType.getId(),
                boxType
        );

        gamer.setProperty("Boxes/Free", 1);
        gamer.sendLocalizedMessage("box_free_given", "%boxName%", boxType.getName(gamer.getLanguage()));
    }

}
