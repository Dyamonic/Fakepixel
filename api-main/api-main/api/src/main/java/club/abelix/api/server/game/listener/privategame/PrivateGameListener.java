package club.abelix.api.server.game.listener.privategame;

import club.abelix.api.base.hotbar.manager.HotbarManager;
import club.abelix.api.base.hotbar_new.HotbarService;
import club.abelix.api.server.game.Game;
import club.abelix.api.server.game.event.GameConnectEvent;
import club.abelix.api.server.game.event.GameJoinEvent;
import club.abelix.api.server.game.event.GameQuitEvent;
import club.abelix.api.server.game.hotbar.GameHotbar;
import club.abelix.api.server.game.listener.GameListener;
import club.abelix.api.server.game.listener.base.JoinResult;
import club.abelix.di.api.annotation.Autowired;
import club.abelix.di.platform.bukkit.DiBukkitPlatform;
import club.abelix.network.common.logging.Logging;
import lombok.val;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;

public class PrivateGameListener extends GameListener<Game> {

    @Autowired
    private HotbarService hotbarService;

    public PrivateGameListener(Game game) {
        super(game);

        DiBukkitPlatform.getFactory().getBeanService().processInjection(this);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onGamerConnect(GameConnectEvent event) {
        checkGame(event, () -> {
            val game = event.getGame();
            val gamer = event.getGamer();

            if (!game.isPrivateGame()) return;
            if (gamer.getParty() == null || gamer.getParty().getId() != game.getPrivateGamePartyId()) {
                event.setResult(JoinResult.PRIVATE_GAME);
            }
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onGamerJoin(GameJoinEvent event) {
        checkGame(event, () -> {
            Logging.IMP.info("[PrivateGame] ({}) Connect gamer {}",
                    event.getGame().getName(), event.getGame().getName());

            val gamer = event.getGamer();

            if(!event.getGame().isPrivateGame())  return;

            hotbarService.apply(GameHotbar.formattedPrivateGamesLobbyHotbarId(game), gamer);
            Logging.IMP.info("[PrivateGame] ({}) Connect gamer {}. Give leader hotbar");
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onGamerQuit(GameQuitEvent event) {
        checkGame(event, () -> {
            val game = event.getGame();

            int afterLeave = game.getGamers().size() - 1;

            if (afterLeave <= 0 && game.isPrivateGame()) {
                game.disablePrivateGameMode(true);
            }

        });
    }
}
