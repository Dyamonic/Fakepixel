package club.abelix.api.nms.bossbar.listener;

import club.abelix.api.nms.NmsConstant;
import club.abelix.api.nms.bossbar.FakeBossbar;
import club.abelix.api.nms.bossbar.FakeBossbarApi;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

@RequiredArgsConstructor
public class FakeBossbarListener implements Listener {

    private final FakeBossbarApi api;

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (NmsConstant.c_customBossbarSending) {
            return;
        }

        for (FakeBossbar bossbar : api.getBossbars()) {
            if (!bossbar.isPublicStatus()) {
                continue;
            }

            bossbar.addReceiver(event.getPlayer());
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        for (FakeBossbar bossbar : api.getBossbars(player)) {
            bossbar.removeReceiver(player);
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        for (FakeBossbar bossbar : api.getBossbars(player)) {
            bossbar.updateBossbar(player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldChange(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();

        for (FakeBossbar bossbar : api.getBossbars(player)) {
            bossbar.update(player);
        }
    }

}
