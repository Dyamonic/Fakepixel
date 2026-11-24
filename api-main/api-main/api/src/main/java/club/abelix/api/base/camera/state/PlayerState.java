package club.abelix.api.base.camera.state;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.item.Applicable;
import lombok.RequiredArgsConstructor;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

@RequiredArgsConstructor
public class PlayerState implements Applicable {

    private final Collection<PotionEffect> potionEffects;
    private final boolean allowFlight;
    private final boolean flying;
    private final GameMode gameMode;

    public PlayerState(Player player) {
        this(player.getActivePotionEffects(), player.getAllowFlight(), player.isFlying(), player.getGameMode());
    }

    @Override
    public void apply(@NotNull Gamer gamer) {
        Player player = gamer.getPlayer();

        // сначала удаляем предыдущие эффекты
        for (PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }

        // потом отправляем новые
        for (PotionEffect effect : potionEffects) {
            effect.apply(player);
        }

        player.setGameMode(gameMode);

        player.setAllowFlight(allowFlight);
        player.setFlying(flying);
    }
}
