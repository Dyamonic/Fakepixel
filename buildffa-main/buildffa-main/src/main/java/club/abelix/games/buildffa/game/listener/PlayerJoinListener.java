package club.abelix.games.buildffa.game.listener;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.listener.YListener;
import club.abelix.games.buildffa.game.BuildFFAGame;
import club.abelix.games.buildffa.game.board.BuildFFABoard;
import lombok.val;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jetbrains.annotations.NotNull;
import org.spigotmc.event.player.PlayerSpawnLocationEvent;

import static club.abelix.games.buildffa.game.data.BuildFFAGamer.SELECTED_KIT_PROPERTY_NAME;

public class PlayerJoinListener extends YListener implements Listener {
    private final BuildFFAGame game;

    public PlayerJoinListener(final @NotNull BuildFFAGame game) {
        super(game.getPlugin());

        this.game = game;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        val player = event.getPlayer();
        val gamer = Gamer.getGamer(player);

        new BuildFFABoard(game, gamer);

        game.setupPlayerInventory(player);
        game.teleportToSpawn(player);
        game.spawnReceivable(player);
    }

    @EventHandler
    public void onPlayerSpawn(final PlayerSpawnLocationEvent event) {
        event.setSpawnLocation(game.resolveSpawnLocation());
    }
}
