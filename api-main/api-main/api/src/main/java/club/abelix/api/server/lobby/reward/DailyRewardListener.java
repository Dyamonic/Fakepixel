package club.abelix.api.server.lobby.reward;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.listener.YListener;
import club.abelix.core.bukkit.HeartBeat;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * @author fleyer (Fleyer001)
 * @since 25.06.2024 at 10:43</p>
 * For AG-Api project.
 */

@RequiredArgsConstructor
public class DailyRewardListener extends YListener {

    private final DailyRewardNotifyService dailyRewardNotifyService;

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        val gamer = Gamer.getGamer(event.getPlayer());

        dailyRewardNotifyService.check(gamer);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        val gamer = Gamer.getGamer(event.getPlayer());

        dailyRewardNotifyService.remove(gamer);
    }
}
