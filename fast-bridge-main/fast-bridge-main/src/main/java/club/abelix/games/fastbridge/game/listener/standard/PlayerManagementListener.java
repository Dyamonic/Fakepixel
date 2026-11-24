package club.abelix.games.fastbridge.game.listener.standard;

import club.abelix.games.fastbridge.commons.FastBridgeGamer;
import club.abelix.games.fastbridge.commons.island.IslandSelector;
import club.abelix.games.fastbridge.commons.island.IslandService;
import club.abelix.games.fastbridge.game.FastBridgeGame;
import club.abelix.games.fastbridge.game.board.FastBridgeBoard;
import club.abelix.games.fastbridge.game.listener.FastBridgeListener;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.log4j.Log4j2;
import lombok.val;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

@Log4j2
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PlayerManagementListener extends FastBridgeListener {

    IslandSelector islandSelector;
    IslandService islandService;

    public PlayerManagementListener(
            final FastBridgeGame game,
            final IslandSelector selector,
            final IslandService islandService
    ) {
        super(game);

        this.islandSelector = selector;
        this.islandService = islandService;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        val gamer = FastBridgeGamer.get(event.getPlayer());
        val player = event.getPlayer();
        val found = islandSelector.findFreeIsland();

        if (found.isEmpty()) {
            log.error("Unable to find suitable island for player {}", player.getName());
            player.kickPlayer("Unable to find suitable island for you");
            return;
        }

        val island = found.get();
        log.info("Found island {} for player {}", island, player.getName());

        islandService.joinIsland(island, gamer);

        FastBridgeBoard.createForGamer(gamer);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        val player = event.getPlayer();
        val gamer = FastBridgeGamer.get(player);

        islandService.exitIsland(gamer.getIsland());
    }
}
