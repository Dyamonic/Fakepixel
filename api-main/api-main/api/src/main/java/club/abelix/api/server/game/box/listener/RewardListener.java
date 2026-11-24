package club.abelix.api.server.game.box.listener;

import club.abelix.api.base.box.reward.service.RewardSelectorService;
import club.abelix.api.server.game.box.event.RewardWeightApplyEvent;
import club.abelix.api.server.game.event.GameEndEvent;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.log4j.Log4j2;
import lombok.val;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

@Log4j2
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class RewardListener implements Listener {

    RewardSelectorService rewardSelectorService;

    @EventHandler
    public void onGameEnd(GameEndEvent event) {
        val game = event.getGame();

        if (game.isPrivateGame()) return;

        if (game.isStatsSavingDisabled()) return;

        val weightService = game.getGameWeightStorage();

        game.getWinner().getPlayers().forEach(gamer -> {
            val weight = Math.min(weightService.getWeight(gamer), 100);

            log.info("Giving reward to player {} with weight {} in game {}",
                    gamer.getName(),
                    weight,
                    game.getName()
            );
            rewardSelectorService.giveReward(gamer, weight);
        });
    }

    @EventHandler
    public void onWeightApply(RewardWeightApplyEvent event) {
        val game = event.getGame();
        val weightService = game.getGameWeightStorage();
        val gamer = event.getGamer();

        if (gamer == null) return;

        weightService.addWeight(event.getGamer(), event.getWeight());
    }

}
