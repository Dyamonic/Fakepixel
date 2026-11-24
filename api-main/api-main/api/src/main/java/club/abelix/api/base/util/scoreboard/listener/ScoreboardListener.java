package club.abelix.api.base.util.scoreboard.listener;

import club.abelix.api.base.listener.YListener;
import club.abelix.api.base.util.scoreboard.Board;
import lombok.NonNull;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

public class ScoreboardListener extends YListener {

    public ScoreboardListener(@NonNull Plugin plugin) {
        super(plugin);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        Board board = Board.getPlayerBoard(player);

        if (board != null) {
            board.removePlayer(player);
        }
    }

}
