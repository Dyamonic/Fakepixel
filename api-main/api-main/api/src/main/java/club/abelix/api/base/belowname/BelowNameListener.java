package club.abelix.api.base.belowname;

import club.abelix.api.base.listener.YListener;
import club.abelix.api.server.game.event.GameQuitEvent;
import lombok.NonNull;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

/**
 * @author artem
 */
public class BelowNameListener extends YListener {

    public BelowNameListener(@NonNull Plugin plugin) {
        super(plugin);
    }

    private void onQuit(Player player) {
        for (BelowName board : BelowNameManager.IMP.getBelowNames()) {
            board.removeScore(player);
        }

        BelowName board = BelowNameManager.IMP.getBelowName(player);

        if (board == null) {
            return;
        }

        board.removeReceiver(player);
        board.removeScore(player);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        onQuit(event.getPlayer());
    }

    @EventHandler
    public void onQuit(GameQuitEvent e) {
        // это не смена игры, значит он вышел с сервера.
        // мы уже смотрим PlayerQuitEvent, так что пропускаем
        if (!e.isChange()) {
            return;
        }

        onQuit(e.getGamer().getPlayer());
    }
}
