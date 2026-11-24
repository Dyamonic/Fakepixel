package club.abelix.api.server.game.task;

import club.abelix.api.base.gamer.Gamer;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.function.Consumer;

@RequiredArgsConstructor
public class GameTimeCoinsTask extends BukkitRunnable {

    private final Consumer<Gamer> rewardConsumer;

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            Gamer gamer = Gamer.getGamer(player);

            if (gamer == null || gamer.isSpectator()) {
                continue;
            }

            //игрок прожил еще минуту, выполняем нужное действие
            rewardConsumer.accept(gamer);
        }
    }

}
