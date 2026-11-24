package club.abelix.api.server.game.listener.game;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.gamer.event.GamerVanishEvent;
import club.abelix.api.base.party.Party;
import club.abelix.api.base.party.event.PartyInviteEvent;
import club.abelix.api.base.party.event.PartyKickEvent;
import club.abelix.api.base.party.event.PartyLoadEvent;
import club.abelix.api.server.game.Game;
import club.abelix.api.server.game.event.GameJoinEvent;
import club.abelix.api.server.game.event.GameQuitEvent;
import club.abelix.api.server.game.event.GameStartEvent;
import club.abelix.api.server.game.listener.GameListener;
import club.abelix.api.server.game.manager.GameGroupSelectorManager;
import club.abelix.api.server.game.team.selector.SelectorGui;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.Arrays;
import java.util.Collections;

public class GameGroupSelectorListener extends GameListener<Game> {

    public GameGroupSelectorListener(Game game) {
        super(game);
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent event) {
        checkGame(event, () -> {
            String message = event.getMessage();

            if (!message.startsWith("/selector")) {
                return;
            }

            event.setCancelled(true);

            Gamer gamer = Gamer.getGamer(event.getPlayer());

            if (gamer == null) {
                return;
            }

            if (gamer.isVanished()) {
                gamer.sendLocalizedMessage("team_selector_unavailable_vanished");
                return;
            }

            String[] params = message.split(" ");
            String[] args = Arrays.copyOfRange(params, 1, params.length);

            if (args.length < 2) {
                new SelectorGui(game, gamer);
                return;
            }

            Gamer senderGamer = Gamer.getGamer(args[1]);

            if (senderGamer == null || senderGamer.isVanished()) {
                gamer.sendLocalizedMessage("player_offline");
                return;
            }

            switch (args[0]) {
                case "accept": {
                    game.getSelectorManager().acceptInvite(senderGamer, gamer);
                    break;
                }
                case "reject": {
                    game.getSelectorManager().denyInvite(senderGamer, gamer);
                    break;
                }
            }
        });
    }

    @EventHandler
    public void onStart(GameStartEvent event) {
        checkGame(event, () -> game.getSelectorManager().onGameStart());
    }

    @EventHandler
    public void onPartyInvite(PartyInviteEvent event) {
        checkGame(event.getGamer(), () -> {
            Gamer gamer = event.getGamer();
            Party party = event.getParty();

            for (Gamer member : party.getMembers()) {
                game.getSelectorManager().resetGroup(member);
            }

            game.getSelectorManager().updateGuiExclude(Collections.singleton(gamer));
        });
    }

    @EventHandler
    public void onPartyKick(PartyKickEvent event) {
        checkGame(event.getGamer(), () -> {
            Gamer gamer = event.getGamer();

            game.getSelectorManager().updateGuiExclude(Collections.singleton(gamer));
        });
    }

    private void handleJoin(Gamer gamer) {
        GameGroupSelectorManager selectorManager = game.getSelectorManager();

        // пати загрузилось раньше, чем игрок зашёл в игру
        if (!selectorManager.hasGroup(gamer)) {
            selectorManager.addGroup(gamer, false);
        }
    }

    @EventHandler
    public void onJoin(PartyLoadEvent event) {
        checkGame(event, () -> handleJoin(event.getGamer()));
    }

    @EventHandler
    public void onJoin(GameJoinEvent event) {
        checkGame(event, () -> handleJoin(event.getGamer()));
    }

    @EventHandler
    public void onPlayerVanish(GamerVanishEvent event) {
        checkGame(event, () -> {
            if (event.isVanished()) {
                game.getSelectorManager().onLeave(event.getGamer());
            }
        });
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onLeave(GameQuitEvent event) {
        checkGame(event, () -> {
            Gamer gamer = event.getGamer();

            game.getSelectorManager().onLeave(gamer);
            game.getSelectorManager().updateGuiExclude(Collections.singleton(gamer));
        });
    }

}
