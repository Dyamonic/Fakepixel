package club.abelix.api.integration.labymod.task;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.util.core.CoreUtil;
import club.abelix.api.base.util.task.AbstractTask;
import club.abelix.api.base.util.task.registration.TaskRegistrationProvider;
import club.abelix.api.integration.labymod.service.LabyIntegrationService;
import lombok.NonNull;
import lombok.val;
import net.labymod.serverapi.core.model.display.EconomyDisplay;
import net.labymod.serverapi.server.bukkit.LabyModPlayer;

public class LabyEconomyUpdateTask extends AbstractTask {

    private final LabyIntegrationService labyIntegrationService;

    public LabyEconomyUpdateTask(@NonNull TaskRegistrationProvider provider,
                                 @NonNull LabyIntegrationService labyIntegrationService) {
        super(provider);
        this.labyIntegrationService = labyIntegrationService;
    }

    private void sendEconomy(@NonNull Gamer gamer, @NonNull LabyModPlayer labyModPlayer) {
        labyModPlayer.updateCashEconomy(economyDisplay -> economyDisplay.balance(gamer.getGold()));
        if (CoreUtil.getMaster() != null) {
            labyModPlayer.updateBankEconomy(economyDisplay -> {
                economyDisplay.balance(gamer.getCoins());
                economyDisplay.decimalFormat(new EconomyDisplay.DecimalFormat(",", 1000));
                economyDisplay.visible(true);
            });
        } else {
            labyModPlayer.updateBankEconomy(economyDisplay -> {
                economyDisplay.visible(false);
                economyDisplay.decimalFormat(new EconomyDisplay.DecimalFormat(",", 1000));
            });

            labyModPlayer.sendPlayingGameMode(null);
        }
    }

    @Override
    protected void run() {
        labyIntegrationService.getAllGamers().forEach(gamer -> {
            val labyModPlayer = labyIntegrationService.get(gamer);

            if (labyModPlayer == null) {
                return;
            }

            sendEconomy(gamer, labyModPlayer);
        });
    }
}
